package com.astrasoft.havaman

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            var account by remember { mutableStateOf<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(this)) }
            var locationText by remember { mutableStateOf("Unknown") }

            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    try {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                        val acct = task.getResult(ApiException::class.java)
                        account = acct
                        // fetch location after sign-in
                        fetchLocation(account)
                    } catch (e: ApiException) {
                        account = null
                    }
                }
            }

            SignInScreen(
                account = account,
                onSignIn = {
                    val signInIntent = googleClient.signInIntent
                    launcher.launch(signInIntent)
                },
                onSignOut = {
                    googleClient.signOut()
                    account = null
                },
                onFetchLocation = { fetchLocation(account) }
            )
        }
    }

    private fun fetchLocation(account: GoogleSignInAccount?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                // For now, just log and you can send this to backend
                // e.g., BackendApi.fetchWeatherWisdom(it.latitude, it.longitude)
            }
        }
    }
}

