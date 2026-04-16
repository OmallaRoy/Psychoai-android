package com.psychoai.app

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.psychoai.app.api.RetrofitClient
import com.psychoai.app.api.SaveTokenRequest
import com.psychoai.app.navigation.AppNavigation
import com.psychoai.app.ui.theme.PsychoAITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── FIX: Request notification permission (Android 13+) ─
        // On Android 13 (API 33) and above, notifications are
        // blocked by default unless explicitly requested.
        // Without this, FCM push notifications arrive silently
        // on the device and are never shown to the trader.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }

        // ── FIX: Register FCM token on every app open ──────────
        // Tokens can rotate. If the backend holds a stale token,
        // notifications fail silently. Running this on every open
        // ensures the backend always has the current valid token.
        registerFcmToken()

        setContent {
            PsychoAITheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
                    color = Color.Black
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }

    // ── Get FCM token and send to Railway backend ──────────────
    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token obtained: ${token.take(20)}...")

                // Persist locally so other parts of the app can use it
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_FCM_TOKEN, token)
                    .apply()

                // Only send to backend when a user is actually logged in
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    sendTokenToBackend(uid, token)
                } else {
                    // Not logged in yet — AuthViewModel will handle sending
                    // the token after the user completes login
                    Log.d(TAG, "User not logged in — token saved locally only")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token: ${e.message}")
            }
    }

    private fun sendTokenToBackend(uid: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.saveFcmToken(
                    SaveTokenRequest(traderId = uid, token = token)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token saved to backend for uid=$uid")
                } else {
                    Log.e(TAG, "Backend rejected FCM token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM token network error: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG                      = "MainActivity"
        private const val NOTIFICATION_PERMISSION_CODE = 100
        private const val PREFS_NAME               = "psychoai_prefs"
        private const val KEY_FCM_TOKEN            = "fcm_token"
    }
}