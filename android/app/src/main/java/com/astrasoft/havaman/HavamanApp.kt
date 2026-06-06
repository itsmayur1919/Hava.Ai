package com.astrasoft.havaman

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun HavamanApp() {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var cityResults by remember { mutableStateOf(listOf<LocationSearchResult>()) }
    var selectedLocation by remember { mutableStateOf(Coordinates(18.59, 73.74)) }
    var wisdom by remember { mutableStateOf<WeatherWisdom?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedLocation) {
        isLoading = true
        errorMessage = null
        try {
            wisdom = BackendApi.fetchWeatherWisdom(selectedLocation.latitude, selectedLocation.longitude)
        } catch (t: Throwable) {
            Log.e("HavamanApp", "Weather fetch failed", t)
            errorMessage = "Unable to fetch weather. Please check your network."
        } finally {
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF04121F)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Header()

            Spacer(modifier = Modifier.height(16.dp))

            SearchSection(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                onSearch = {
                    if (searchQuery.text.isNotBlank()) {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                cityResults = BackendApi.searchLocation(searchQuery.text)
                                errorMessage = null
                            } catch (t: Throwable) {
                                Log.e("HavamanApp", "Location search failed", t)
                                errorMessage = "Unable to find locations."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            )

            if (cityResults.isNotEmpty()) {
                CityResultsList(cityResults) { result ->
                    selectedLocation = Coordinates(result.latitude, result.longitude)
                    cityResults = emptyList()
                    searchQuery = TextFieldValue(result.name)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HeroPanel(wisdom, selectedLocation, isLoading)
            Spacer(modifier = Modifier.height(16.dp))
            WisdomGrid(wisdom)
            Spacer(modifier = Modifier.height(16.dp))
            RadarPreviewPanel(isLoading)

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SignInScreen(
    account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onFetchLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04121F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App branding
        Text("Havaman.ai", color = Color.White, fontSize = 36.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text("AI Weather Intelligence", color = Color(0xFF4FD1C5), fontSize = 14.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        if (account == null) {
            // Sign-In Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(onClick = onSignIn),
                shape = RoundedCornerShape(12.dp),
                color = Color.White
            ) {
                Text(
                    "Sign in with Google",
                    fontSize = 16.sp,
                    color = Color(0xFF1a1a1a),
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            }
        } else {
            // Welcome message
            Text("Welcome, ${account.displayName}!", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 24.dp))
            
            // Fetch Location Button
            Button(
                onClick = onFetchLocation,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FD1C5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Fetch My Location", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sign Out Button
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Sign Out", color = Color.White)
            }
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text("Havaman.ai", color = Color.White, fontSize = 32.sp)
        Text("AI weather intelligence for every location", color = Color(0xFF8AA7C2), fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSection(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onSearch: () -> Unit) {
    Column {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Search city name") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x16FFFFFF), RoundedCornerShape(18.dp)),
            textStyle = TextStyle(color = Color.White),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = Color(0x14FFFFFF),
                cursorColor = Color(0xFFB8E0FF)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onSearch,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B7CFF)),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Search", color = Color.White)
        }
    }
}

@Composable
private fun CityResultsList(results: List<LocationSearchResult>, onSelect: (LocationSearchResult) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        results.forEach { location ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(location) },
                shape = RoundedCornerShape(14.dp),
                color = Color(0x19FFFFFF)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(location.name, color = Color.White, fontSize = 16.sp)
                    Text(location.country ?: "", color = Color(0xFF9BB0CC), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun HeroPanel(wisdom: WeatherWisdom?, coords: Coordinates, isLoading: Boolean) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x14FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Live Forecast", color = Color(0xFFB8E0FF), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                wisdom?.meta?.temp?.let { "${it.toInt()}°C" } ?: "--°C",
                color = Color.White,
                fontSize = 44.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "UV: ${wisdom?.meta?.uv ?: "--"} • AQI: ${wisdom?.meta?.aqi ?: "--"}",
                color = Color(0xFF8AA7C2)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (isLoading) "Updating weather intelligence..." else "AI-derived health, travel, and clothing guidance",
                color = Color(0xFFB0C8DE),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun WisdomGrid(wisdom: WeatherWisdom?) {
    val cards = listOf(
        WisdomCardState("Health", wisdom?.health_card ?: "No data available", Color(0xFFFC5C7D)),
        WisdomCardState("Travel", wisdom?.travel_card ?: "No data available", Color(0xFFF9C74F)),
        WisdomCardState("Clothing", wisdom?.clothing_card ?: "No data available", Color(0xFF4FD1C5))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        cards.forEach { state ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x14FFFFFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(state.title, color = state.tint, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.description, color = Color(0xFFDAE6F2), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RadarPreviewPanel(isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x14FFFFFF),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size((120 * pulse).dp)
                    .align(Alignment.Center)
                    .background(Color(0x3324A5FF), RoundedCornerShape(120.dp))
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("3D Radar Preview", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isLoading) "Loading map overlay..." else "Realtime precipitation layer ready",
                    color = Color(0xFF9BB0CC),
                    fontSize = 12.sp
                )
            }
        }
    }
}

data class Coordinates(val latitude: Double, val longitude: Double)
data class WisdomCardState(val title: String, val description: String, val tint: Color)
