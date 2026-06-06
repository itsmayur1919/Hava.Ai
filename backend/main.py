import asyncio
from typing import Optional
import os

import httpx
import uuid
from fastapi import FastAPI, Depends, HTTPException, Request, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

from .database import engine, Base, AsyncSessionLocal, get_session
from .models import User, SavedLocation

app = FastAPI(title="Havaman.ai API")

# For local device testing we allow all origins; restrict in production.
origins = ["*"]

app.add_middleware(CORSMiddleware, allow_origins=origins, allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

# Environment control: set TEST_SIGNIN_MODE=test to force test-login handling
TEST_SIGNIN_MODE = os.environ.get("TEST_SIGNIN_MODE", "google").lower()


class GoogleAuthIn(BaseModel):
    token: str


class TestLoginIn(BaseModel):
    username: Optional[str] = None
    password: Optional[str] = None


class AuthIn(BaseModel):
    token: Optional[str] = None
    username: Optional[str] = None
    password: Optional[str] = None


class LocationSearchOut(BaseModel):
    name: str
    latitude: float
    longitude: float
    country: Optional[str]


@app.on_event("startup")
async def startup():
    # Ensure tables exist using the async engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


@app.post("/api/auth/google")
async def google_auth(payload: GoogleAuthIn, session=Depends(get_session)):
    """
    Receives a Google ID token, validates via Google's tokeninfo endpoint, extracts the 'sub' (google_id), email, name, and picture and UPSERTs the user.
    """
    token = payload.token
    async with httpx.AsyncClient(timeout=10) as client:
        try:
            # Use Google's tokeninfo to validate ID token
            r = await client.get("https://oauth2.googleapis.com/tokeninfo", params={"id_token": token})
            r.raise_for_status()
            info = r.json()
        except httpx.HTTPError:
            raise HTTPException(status_code=400, detail="Invalid Google token")

    google_id = info.get("sub")
    email = info.get("email")
    name = info.get("name")
    picture = info.get("picture")

    if not google_id or not email:
        raise HTTPException(status_code=400, detail="Missing identity in token")

    # UPSERT semantics
    async with AsyncSessionLocal() as db:
        q = select(User).where(User.google_id == google_id)
        res = await db.execute(q)
        user = res.scalars().first()
        if user:
            user.email = email
            user.full_name = name
            user.profile_pic = picture
            db.add(user)
            await db.commit()
            await db.refresh(user)
            return {"status": "ok", "user_id": user.id}

        user = User(google_id=google_id, email=email, full_name=name, profile_pic=picture)
        db.add(user)
        try:
            await db.commit()
            await db.refresh(user)
        except IntegrityError:
            await db.rollback()
            # try to fetch existing by email
            q = select(User).where(User.email == email)
            res = await db.execute(q)
            existing = res.scalars().first()
            if existing:
                existing.google_id = google_id
                db.add(existing)
                await db.commit()
                await db.refresh(existing)
                return {"status": "ok", "user_id": existing.id}
            raise

    return {"status": "ok", "user_id": user.id}


@app.post("/api/auth/test-login")
async def test_login(payload: TestLoginIn, session=Depends(get_session)):
    """
    Testing-only endpoint: accepts a username/password (ignored) and creates or returns
    a mock user. The user is stored with google_id set to "test:{username}" so it
    reuses the existing user model requirements.
    """
    username = payload.username or f"user_{str(uuid.uuid4())[:8]}"
    google_id = f"test:{username}"
    email = f"{username}@example.com"
    name = username
    picture = None

    async with AsyncSessionLocal() as db:
        q = select(User).where(User.google_id == google_id)
        res = await db.execute(q)
        user = res.scalars().first()
        if user:
            user.email = email
            user.full_name = name
            db.add(user)
            await db.commit()
            await db.refresh(user)
            return {"status": "ok", "user_id": user.id}

        user = User(google_id=google_id, email=email, full_name=name, profile_pic=picture)
        db.add(user)
        try:
            await db.commit()
            await db.refresh(user)
        except IntegrityError:
            await db.rollback()
            # try to fetch existing by email
            q = select(User).where(User.email == email)
            res = await db.execute(q)
            existing = res.scalars().first()
            if existing:
                existing.google_id = google_id
                db.add(existing)
                await db.commit()
                await db.refresh(existing)
                return {"status": "ok", "user_id": existing.id}
            raise

    return {"status": "ok", "user_id": user.id}


@app.post("/api/auth/login")
async def unified_login(payload: AuthIn, session=Depends(get_session)):
    """
    Unified login endpoint for local testing. Behavior controlled by the
    `TEST_SIGNIN_MODE` environment variable:
      - "test": always use the test-login flow (username/password)
      - "google": prefer Google token flow when `token` is provided
    This makes it easy to point mobile devices at a local backend and
    force a non-Google authentication flow during development.
    """
    mode = TEST_SIGNIN_MODE

    # If mode is 'test', short-circuit to creating a test user
    if mode == "test":
        tl = TestLoginIn(username=payload.username, password=payload.password)
        return await test_login(tl, session=session)

    # Otherwise, if a Google token is provided, validate it
    if payload.token:
        ga = GoogleAuthIn(token=payload.token)
        return await google_auth(ga, session=session)

    # Fallback to test-login if no token provided
    tl = TestLoginIn(username=payload.username, password=payload.password)
    return await test_login(tl, session=session)


@app.get("/api/location/search", response_model=list[LocationSearchOut])
async def geocode(q: str = Query(..., min_length=1)):
    """Translate a city string into coordinates using Open-Meteo geocoding."""
    url = "https://geocoding-api.open-meteo.com/v1/search"
    async with httpx.AsyncClient(timeout=10) as client:
        r = await client.get(url, params={"name": q, "count": 10})
        r.raise_for_status()
        data = r.json()

    results = []
    for item in data.get("results", [])[:10]:
        results.append(LocationSearchOut(name=item.get("name"), latitude=item.get("latitude"), longitude=item.get("longitude"), country=item.get("country")))
    return results


@app.get("/api/weather/wisdom")
async def weather_wisdom(lat: float = Query(...), lon: float = Query(...)):
    """
    Fetches weather, UV index, and AQI then returns three actionable natural language strings: Health Card, Travel Card, Clothing Card.
    Uses Open-Meteo endpoints.
    """
    async with httpx.AsyncClient(timeout=15) as client:
        # Current weather + uv
        forecast_url = "https://api.open-meteo.com/v1/forecast"
        params = {"latitude": lat, "longitude": lon, "current_weather": True, "hourly": "uv_index,precipitation,temperature_2m,windgusts_10m", "timezone": "auto"}
        f_r = await client.get(forecast_url, params=params)
        f_r.raise_for_status()
        fdata = f_r.json()

        # Air quality (Open-Meteo Air Quality API)
        aqi_url = "https://air-quality-api.open-meteo.com/v1/air-quality"
        a_r = await client.get(aqi_url, params={"latitude": lat, "longitude": lon, "hourly": "us_aqi,pm10,pm2_5"})
        a_r.raise_for_status()
        adata = a_r.json()

    # Basic rule-based parsing
    health_msgs = []
    travel_msgs = []
    clothing_msgs = []

    # UV logic
    current_uv = None
    try:
        # pick current hour index if available
        uv_series = fdata.get("hourly", {}).get("uv_index", [])
        if uv_series:
            # assume first hour corresponds to current if timezone auto
            current_uv = float(uv_series[0])
    except Exception:
        current_uv = None

    if current_uv is not None:
        if current_uv > 8:
            health_msgs.append("High UV Alert: Apply SPF 30+ immediately and limit direct sun exposure.")
            clothing_msgs.append("Wear long sleeves and broad-brim hat; UV-protective sunglasses recommended.")
        elif current_uv > 6:
            health_msgs.append("Moderate-High UV: Use sunscreen and reapply every 2 hours.")
            clothing_msgs.append("Light layers with UV protection recommended.")
        else:
            health_msgs.append("UV levels are moderate/low. Standard sun precautions apply.")

    # AQI logic
    try:
        aqi_series = adata.get("hourly", {}).get("us_aqi", [])
        aqi = int(aqi_series[0]) if aqi_series else None
    except Exception:
        aqi = None

    if aqi is not None:
        if aqi >= 151:
            health_msgs.append("Air Quality Unhealthy: Avoid outdoor exertion and consider an N95 mask.")
            travel_msgs.append("Expect restricted outdoor activities; consider rescheduling long drives.")
        elif aqi >= 101:
            health_msgs.append("Air Quality Moderate-Unhealthy for Sensitive Groups: Sensitive people should limit prolonged outdoor exertion.")
        else:
            health_msgs.append("Air quality is good to moderate.")

    # Precipitation / wind -> Travel and clothing
    precip = None
    windgust = None
    try:
        precip_series = fdata.get("hourly", {}).get("precipitation", [])
        wind_series = fdata.get("hourly", {}).get("windgusts_10m", [])
        precip = float(precip_series[0]) if precip_series else 0.0
        windgust = float(wind_series[0]) if wind_series else 0.0
    except Exception:
        precip = 0.0
        windgust = 0.0

    if precip and precip > 0.5:
        travel_msgs.append("Precipitation expected: Bring waterproof gear; road conditions may be slick.")
        clothing_msgs.append("Waterproof jacket and shoes recommended.")
    else:
        travel_msgs.append("No significant precipitation predicted in the short term.")

    if windgust and windgust > 25:
        travel_msgs.append("High wind advisory: Secure outdoor items and expect gusty travel conditions.")
        health_msgs.append("Wind hazard: Loose objects and debris may pose risk; exercise caution.")

    # Temperature-based clothing advice
    temp = None
    try:
        current_weather = fdata.get("current_weather", {})
        temp = float(current_weather.get("temperature")) if current_weather else None
    except Exception:
        temp = None

    if temp is not None:
        if temp <= 5:
            clothing_msgs.append("Very cold: Thermal layers and insulated coat required.")
        elif temp <= 15:
            clothing_msgs.append("Cool: Layer up with a warm jacket.")
        elif temp <= 25:
            clothing_msgs.append("Mild: Light layers are comfortable.")
        else:
            clothing_msgs.append("Hot: Breathable, light fabrics and stay hydrated.")

    # Consolidate top messages
    health_card = " ".join(health_msgs) if health_msgs else "No specific health alerts."
    travel_card = " ".join(travel_msgs) if travel_msgs else "No travel advisories."
    clothing_card = " ".join(clothing_msgs) if clothing_msgs else "No special clothing recommendations."

    return {"health_card": health_card, "travel_card": travel_card, "clothing_card": clothing_card, "meta": {"aqi": aqi, "uv": current_uv, "temp": temp}}
