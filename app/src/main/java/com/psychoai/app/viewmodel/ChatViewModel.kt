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

    // ── Conversation history list ──────────────────────────────
    // Stores every user and assistant message for the current session.
    // Sent with every /chat request so Groq sees full context and
    // can respond to follow-ups like "yes" correctly instead of
    // re-introducing itself every time.
    private val conversationHistory = mutableListOf<Map<String, String>>()

    init {
        // Welcome message shown when chat opens.
        // NOT added to conversationHistory — local UI only.
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

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()

        // Build user message for the UI
        val userMessage = ChatMessage(
            id        = UUID.randomUUID().toString(),
            sender    = MessageSender.USER,
            text      = trimmed,
            tradeTags = extractTradeTags(trimmed)
        )

        // Append user turn to history BEFORE the API call so the
        // current message is included in the history sent to backend
        conversationHistory.add(mapOf("role" to "user", "content" to trimmed))

        // Show user message and typing indicator immediately
        _uiState.update {
            it.copy(messages = it.messages + userMessage, isTyping = true)
        }

        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: "anonymous"

                // FIX: was RetrofitClient.api.sendChat(...)
                // Renamed to chatWithPlutus to match PsychoaiApiService
                val response = RetrofitClient.api.chatWithPlutus(
                    ChatRequest(
                        traderId = uid,
                        message  = trimmed,
                        messages = conversationHistory.toList() // full history
                    )
                )

                if (response.isSuccessful) {
                    val replyText = response.body()?.response
                        ?: "I didn't catch that — could you rephrase?"

                    // Append Plutus reply to history so on the NEXT
                    // message Groq sees what was said here
                    conversationHistory.add(
                        mapOf("role" to "assistant", "content" to replyText)
                    )

                    val aiMessage = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = replyText
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + aiMessage,
                            isTyping = false
                        )
                    }

                } else {
                    // Backend returned HTTP error — remove the failed
                    // user message from history to avoid corruption
                    conversationHistory.removeLastOrNull()

                    val errorMsg = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = "I'm having trouble connecting right now. " +
                                "Please try again in a moment."
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMsg,
                            isTyping = false
                        )
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "sendMessage error: ${e.message}")

                // Remove the failed user message from history
                conversationHistory.removeLastOrNull()

                val errorMsg = ChatMessage(
                    id     = UUID.randomUUID().toString(),
                    sender = MessageSender.AI,
                    text   = "Network error. Please check your connection " +
                            "and try again."
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMsg,
                        isTyping = false
                    )
                }
            }
        }
    }

    fun sendQuickAction(action: String) {
        sendMessage(action)
    }

    // Called from ChatScreen when "New Chat" is tapped in the drawer
    fun startNewSession() {
        // Clear history so Groq starts completely fresh
        conversationHistory.clear()

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

    // Extract ticker symbol tags from message text
    private fun extractTradeTags(text: String): List<TradeTag> {
        val tags  = mutableListOf<TradeTag>()
        val regex = Regex("\\\$([A-Z]{1,5})")
        regex.find(text)?.let {
            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
        }
        return tags
    }
}