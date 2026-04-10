package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.psychoai.app.model.ChatMessage
import com.psychoai.app.model.MessageSender
import com.psychoai.app.model.QuickAction
import com.psychoai.app.model.TradeTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val sessionDate: String = LocalDate.now().format(
        DateTimeFormatter.ofPattern("MMMM d, yyyy")
            .withLocale(java.util.Locale.ENGLISH)
    ).uppercase(),
    val quickActions: List<QuickAction> = listOf(
        QuickAction("Fix my risk"),
        QuickAction("Analyze \$TSLA"),
        QuickAction("Mindset Check"),
        QuickAction("Journal")
    ),
    val hasInsight: Boolean = false,
    val insightMessage: String = ""
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val psychoaiViewModel = PsychoaiViewModel()
    private val db = FirebaseFirestore.getInstance()
    private var sessionFirstMessage: String? = null

    init {
        psychoaiViewModel.registerFcmToken()
    }

    // ── Start a fresh session ──────────────────────────────────
    fun startNewSession() {
        _uiState.update {
            ChatUiState(
                sessionDate = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        .withLocale(java.util.Locale.ENGLISH)
                ).uppercase()
            )
        }
        sessionFirstMessage = null
    }

    // ── Send message ───────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id        = UUID.randomUUID().toString(),
            sender    = MessageSender.USER,
            text      = text.trim(),
            tradeTags = extractTradeTags(text)
        )

        if (sessionFirstMessage == null) {
            sessionFirstMessage = text.trim()
        }

        _uiState.update {
            it.copy(
                messages   = it.messages + userMessage,
                isTyping   = true,
                hasInsight = false
            )
        }

        viewModelScope.launch {
            psychoaiViewModel.chatWithPlutus(
                message    = text,
                onResponse = { responseText ->
                    val aiMessage = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = responseText
                    )
                    _uiState.update {
                        it.copy(
                            messages       = it.messages + aiMessage,
                            isTyping       = false,
                            hasInsight     = true,
                            insightMessage = "Plutus has analyzed your trading psychology."
                        )
                    }
                    saveChatSession(text, responseText)
                },
                onError = { _ ->
                    val fallback = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = generateFallbackResponse(text)
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + fallback,
                            isTyping = false
                        )
                    }
                }
            )
        }
    }

    // ── Save chat session to Firestore ─────────────────────────
    // Saved under chat_sessions/{uid}/sessions so history drawer can load it
    private fun saveChatSession(userMessage: String, aiResponse: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val entry = hashMapOf(
                    "type"             to "chat",
                    "date"             to LocalDate.now().toString(),
                    "timestamp"        to System.currentTimeMillis(),
                    "first_message"    to userMessage.take(120),
                    "ai_snippet"       to aiResponse.take(200),
                    "pattern"          to "",
                    "confidence"       to 0.0,
                    "coaching_snippet" to aiResponse.take(200)
                )
                db.collection("chat_sessions")
                    .document(uid)
                    .collection("sessions")
                    .add(entry)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Failed to save session: ${e.message}")
            }
        }
    }

    fun sendQuickAction(action: String) = sendMessage(action)

    private fun extractTradeTags(text: String): List<TradeTag> {
        val tags = mutableListOf<TradeTag>()
        val symbolRegex = Regex("\\\$([A-Z]{1,5})")
        symbolRegex.find(text)?.let {
            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
        }
        return tags
    }

    private fun generateFallbackResponse(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("risk") ->
                "Based on your recent trades, you appear to be risking above your stated limit. Review your position sizing rules before your next session."
            lower.contains("fomo") ->
                "FOMO entries typically occur after the optimal entry has passed. Your historical FOMO trades show a lower win rate than your planned entries."
            lower.contains("mindset") ->
                "Your mindset score suggests elevated stress. Consider a short break before your next trade to reset your emotional baseline."
            else ->
                "I am analyzing your trading psychology. Please ensure your backend connection is active for full Plutus coaching."
        }
    }
}