package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.psychoai.app.api.AnalyzeRequest
import com.psychoai.app.api.ChatRequest
import com.psychoai.app.api.RetrofitClient
import com.psychoai.app.api.SaveTokenRequest
import com.psychoai.app.api.TradeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PsychoaiState(
    val isLoading: Boolean = false,
    val coachingText: String? = null,
    val predictedMistake: String? = null,
    val confidence: Double = 0.0,
    val featureSignals: List<String> = emptyList(),
    val error: String? = null
)

class PsychoaiViewModel : ViewModel() {

    private val api = RetrofitClient.api
    private val _state = MutableStateFlow(PsychoaiState())
    val state: StateFlow<PsychoaiState> = _state

    private fun getTraderId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    // Get display name — used for natural conversation with Plutus
    // Falls back to first name, then uid, then anonymous
    private fun getTraderDisplayName(): String {
        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName
        return when {
            !displayName.isNullOrBlank() -> displayName.split(" ").first()
            else -> user?.uid ?: "anonymous"
        }
    }

    // ── Chat with Plutus ───────────────────────────────────────
    // Sends the trader's display name so Plutus greets them naturally
    fun chatWithPlutus(
        message: String,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = api.chatWithPlutus(
                    ChatRequest(
                        // Use display name so Plutus says "Hey Roy" not UID
                        traderId = getTraderDisplayName(),
                        message  = message
                    )
                )
                if (response.isSuccessful) {
                    val text = response.body()?.response ?: "No response"
                    onResponse(text)
                } else {
                    onError("Backend error: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("Network error: ${e.message}")
            }
        }
    }

    // ── Analyze a trade ───────────────────────────────────────
    fun analyzeTrade(trade: TradeData) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val token = getFcmToken()
                val response = api.analyzeTrade(
                    AnalyzeRequest(
                        traderId = getTraderId(),
                        fcmToken = token,
                        trade    = trade
                    )
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    _state.value = _state.value.copy(
                        isLoading        = false,
                        predictedMistake = body?.predictedMistake,
                        confidence       = body?.confidence ?: 0.0,
                        featureSignals   = body?.featureSignals ?: emptyList()
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error     = "Error: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = e.message
                )
            }
        }
    }

    // ── Save FCM token ────────────────────────────────────────
    fun registerFcmToken() {
        viewModelScope.launch {
            try {
                val token = getFcmToken() ?: return@launch
                api.saveFcmToken(
                    SaveTokenRequest(
                        traderId = getTraderId(),
                        token    = token
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PsychoaiVM", "FCM token error: ${e.message}")
            }
        }
    }

    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            null
        }
    }
}