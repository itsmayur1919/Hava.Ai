package com.astrasoft.havaman

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    companion object {
        // Default backend host for emulator. For a physical device set this to your machine IP (e.g. http://192.168.1.10:8000)
        const val BACKEND_BASE = "http://10.0.2.2:8000"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var signedIn by remember { mutableStateOf(false) }
            var loggedInUser by remember { mutableStateOf<String?>(null) }

            if (!signedIn) {
                SignInScreen(
                    accountDisplayName = null,
                    onSignIn = { username, password ->
                        lifecycleScope.launch {
                            val success = try {
                                postTestLogin(BACKEND_BASE + "/api/auth/test-login", username, password)
                            } catch (t: Throwable) {
                                false
                            }
                            if (success) {
                                loggedInUser = username
                                signedIn = true
                            }
                        }
                    },
                    onSignOut = {},
                    onFetchLocation = {}
                )
            } else {
                HavamanApp(accountDisplayName = loggedInUser)
            }
        }
    }

    private suspend fun postTestLogin(urlStr: String, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val payload = "{\"username\":\"${username}\",\"password\":\"${password}\"}"
            try {
                BufferedOutputStream(conn.outputStream).use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                    out.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                        val sb = StringBuilder()
                        var line: String? = reader.readLine()
                        while (line != null) {
                            sb.append(line)
                            line = reader.readLine()
                        }
                        // simple check for a returned user_id or status
                        val body = sb.toString()
                        return@withContext (body.contains("\"status\":\"ok\"") || body.contains("user_id"))
                    }
                }
                false
            } finally {
                conn.disconnect()
            }
        }
    }
}

// Simple settings types used by SettingsStorage
enum class LanguageOption { English, Spanish }
enum class TemperatureUnit { Celsius, Fahrenheit }

data class SettingsState(
    val apiKey: String,
    val language: LanguageOption,
    val temperatureUnit: TemperatureUnit,
    val hapticFeedback: Boolean,
    val textToSpeech: Boolean
)

object SettingsStorage {
    private const val SHARED_PREFS = "hava_prefs"
    private const val KEY_API_KEY = "openweather_api_key"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_UNIT = "temperature_unit"
    private const val KEY_HAPTIC = "haptic_feedback"
    private const val KEY_TTS = "text_to_speech"

    fun load(context: Context): SettingsState {
        val prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val language = when (prefs.getString(KEY_LANGUAGE, LanguageOption.English.name)) {
            LanguageOption.Spanish.name -> LanguageOption.Spanish
            else -> LanguageOption.English
        }
        val unit = when (prefs.getString(KEY_UNIT, TemperatureUnit.Celsius.name)) {
            TemperatureUnit.Fahrenheit.name -> TemperatureUnit.Fahrenheit
            else -> TemperatureUnit.Celsius
        }
        val haptic = prefs.getBoolean(KEY_HAPTIC, true)
        val tts = prefs.getBoolean(KEY_TTS, false)
        return SettingsState(apiKey = apiKey, language = language, temperatureUnit = unit, hapticFeedback = haptic, textToSpeech = tts)
    }

    fun save(context: Context, settings: SettingsState) {
        val prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_LANGUAGE, settings.language.name)
            .putString(KEY_UNIT, settings.temperatureUnit.name)
            .putBoolean(KEY_HAPTIC, settings.hapticFeedback)
            .putBoolean(KEY_TTS, settings.textToSpeech)
            .apply()
    }
}

