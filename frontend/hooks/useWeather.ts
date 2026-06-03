import { useEffect, useState, useCallback } from 'react'
import { Geolocation } from '@capacitor/geolocation'
import { Toast } from '@capacitor/toast'
import axios from 'axios'

type User = { id?: number; full_name?: string; email?: string; profile_pic?: string }

type Coordinates = { latitude: number; longitude: number }

const FALLBACK: Coordinates = { latitude: 18.59, longitude: 73.74 }

export function useWeather() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [currentCoordinates, setCurrentCoordinates] = useState<Coordinates | null>(null)
  const [weatherWisdom, setWeatherWisdom] = useState<any>(null)
  const [weatherRadarTimeline, setWeatherRadarTimeline] = useState<any>(null)

  const fetchWisdom = useCallback(async (lat: number, lon: number) => {
    try {
      const r = await axios.get(`/api/weather/wisdom?lat=${lat}&lon=${lon}`)
      setWeatherWisdom(r.data)
    } catch (e) {
      console.error('Failed to fetch wisdom', e)
    }
  }, [])

  useEffect(() => {
    const init = async () => {
      try {
        const perm = await Geolocation.checkPermissions()
        if (perm.location === 'granted' || perm.location === 'prompt') {
          const pos = await Geolocation.getCurrentPosition()
          const coords = { latitude: pos.coords.latitude, longitude: pos.coords.longitude }
          setCurrentCoordinates(coords)
          fetchWisdom(coords.latitude, coords.longitude)
        } else {
          // user denied
          await Toast.show({ text: `User has declined location access.` })
          setCurrentCoordinates(FALLBACK)
          fetchWisdom(FALLBACK.latitude, FALLBACK.longitude)
        }
      } catch (err) {
        await Toast.show({ text: `Location error. Using fallback coordinates.` })
        setCurrentCoordinates(FALLBACK)
        fetchWisdom(FALLBACK.latitude, FALLBACK.longitude)
      }
    }

    init()
  }, [fetchWisdom])

  return {
    currentUser,
    setCurrentUser,
    currentCoordinates,
    setCurrentCoordinates,
    weatherWisdom,
    weatherRadarTimeline,
    fetchWisdom,
  }
}

export default useWeather