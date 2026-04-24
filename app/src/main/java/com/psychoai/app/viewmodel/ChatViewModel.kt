package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.psychoai.app.api.ChatRequest
import com.psychoai.app.api.RetrofitClient
import com.psychoai.app.model.ChatMessage
import com.psychoai.app.model.MessageSender
import com.psychoai.app.model.QuickAction
import com.psychoai.app.model.TradeTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        QuickAction("Stop revenge trading"),
        QuickAction("Mindset Check"),
        QuickAction("Journal review")
    ),
    val hasInsight: Boolean = false,
    val insightMessage: String = ""
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // ── Conversation history (v1.1 stateless fix) ─────────────
    // Every user and assistant message is stored here and sent
    // with every /chat call so Groq has full context.
    // Fixes: Plutus re-introducing itself after "yes", "go ahead".
    private val conversationHistory = mutableListOf<Map<String, String>>()

    // ── Last detected pattern (v1.2 Tavily context) ───────────
    // Passed to the backend with every /chat call so Tavily can
    // build a more targeted web search query.
    private var lastDetectedPattern: String = ""

    init {
        // Welcome message — shown in UI only, NOT added to
        // conversationHistory so Groq does not count it as context.
        val welcome = ChatMessage(
            id     = UUID.randomUUID().toString(),
            sender = MessageSender.AI,
            text   = "Hello! I'm Plutus, your trading psychology coach. " +
                    "You can chat with me, upload a trade journal " +
                    "(CSV or Excel), or ask about your trading patterns. " +
                    "How can I help you today?"
        )
        _uiState.update { it.copy(messages = listOf(welcome)) }
    }

    // ── Public: update last detected pattern ──────────────────
    fun updateLastDetectedPattern(pattern: String) {
        if (pattern.isNotBlank()) {
            lastDetectedPattern = pattern
        }
    }

    // ── Send message ───────────────────────────────────────────
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        val userMessage = ChatMessage(
            id        = UUID.randomUUID().toString(),
            sender    = MessageSender.USER,
            text      = trimmed,
            tradeTags = extractTradeTags(trimmed)
        )

        conversationHistory.add(mapOf("role" to "user", "content" to trimmed))

        _uiState.update {
            it.copy(
                messages   = it.messages + userMessage,
                isTyping   = true,
                hasInsight = false
            )
        }

        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: "anonymous"

                val response = RetrofitClient.api.chatWithPlutus(
                    ChatRequest(
                        traderId    = uid,
                        message     = trimmed,
                        messages    = conversationHistory.toList(),
                        lastPattern = lastDetectedPattern
                    )
                )

                if (response.isSuccessful) {
                    val replyText = response.body()?.response
                        ?: "I didn't catch that — could you rephrase?"

                    conversationHistory.add(
                        mapOf("role" to "assistant", "content" to replyText)
                    )

                    // Keep history bounded to last 40 messages
                    if (conversationHistory.size > 40) {
                        val excess = conversationHistory.size - 40
                        repeat(excess) { conversationHistory.removeAt(0) }
                    }

                    val aiMessage = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = replyText
                    )

                    _uiState.update {
                        it.copy(
                            messages       = it.messages + aiMessage,
                            isTyping       = false,
                            hasInsight     = true,
                            insightMessage = "Plutus has analyzed your trading psychology."
                        )
                    }

                    // ── NOTE: Do NOT save to Firestore here ────
                    // The backend's /chat endpoint already saves the
                    // exchange to the root chat_sessions collection
                    // with trader_id field — exactly where the
                    // GET /trader/{id}/history endpoint looks for it.
                    //
                    // The previous saveChatSession() function wrote to
                    // chat_sessions/{uid}/sessions (a subcollection)
                    // which the history endpoint could NEVER query,
                    // causing the history drawer to always be empty.
                    // Removed entirely — backend handles persistence.

                } else {
                    conversationHistory.removeLastOrNull()

                    val errorMsg = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = "I'm having trouble connecting right now. " +
                                "Please try again in a moment."
                    )
                    _uiState.update {
                        it.copy(messages = it.messages + errorMsg, isTyping = false)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "sendMessage error: ${e.message}")
                conversationHistory.removeLastOrNull()

                val errorMsg = ChatMessage(
                    id     = UUID.randomUUID().toString(),
                    sender = MessageSender.AI,
                    text   = "Network error. Please check your connection " +
                            "and try again."
                )
                _uiState.update {
                    it.copy(messages = it.messages + errorMsg, isTyping = false)
                }
            }
        }
    }

    fun sendQuickAction(action: String) {
        sendMessage(action)
    }

    // ── New chat / clear session ───────────────────────────────
    fun startNewSession() {
        conversationHistory.clear()
        lastDetectedPattern = ""

        val welcome = ChatMessage(
            id     = UUID.randomUUID().toString(),
            sender = MessageSender.AI,
            text   = "Starting a new session. I'm Plutus, your trading " +
                    "psychology coach. What would you like to work on today?"
        )

        _uiState.update {
            it.copy(
                messages       = listOf(welcome),
                isTyping       = false,
                hasInsight     = false,
                insightMessage = "",
                sessionDate    = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        .withLocale(java.util.Locale.ENGLISH)
                ).uppercase()
            )
        }
    }

    private fun extractTradeTags(text: String): List<TradeTag> {
        val tags  = mutableListOf<TradeTag>()
        val regex = Regex("\\\$([A-Z]{1,5})")
        regex.find(text)?.let {
            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
        }
        return tags
    }
}