package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.psychoai.app.api.ChatRequest
import com.psychoai.app.api.ChatSession
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
    val messages:        List<ChatMessage> = emptyList(),
    val isTyping:        Boolean = false,
    val sessionDate:     String  = today(),
    val quickActions:    List<QuickAction> = listOf(
        QuickAction("Fix my risk"),
        QuickAction("Stop revenge trading"),
        QuickAction("Mindset Check"),
        QuickAction("Journal review")
    ),
    val hasInsight:      Boolean = false,
    val insightMessage:  String  = "",
    // Current session title — updated after first exchange
    val sessionTitle:    String  = "New Chat",
    val currentSessionId: String = ""
)

private fun today(): String = LocalDate.now().format(
    DateTimeFormatter.ofPattern("MMMM d, yyyy")
        .withLocale(java.util.Locale.ENGLISH)
).uppercase()

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Full back-and-forth sent with every /chat call (stateless fix)
    private val conversationHistory = mutableListOf<Map<String, String>>()

    // Last detected pattern — for targeted Tavily queries
    private var lastDetectedPattern: String = ""

    // Current session UUID — same for every message in this conversation
    private var currentSessionId: String = UUID.randomUUID().toString()

    // Whether the first exchange has happened in this session
    // The title is generated after the first AI reply
    private var isFirstExchangeDone: Boolean = false

    // Firebase display name — fetched once, used in every ChatRequest
    private var username: String = ""

    init {
        fetchUsername()
        startFreshSession()
    }

    // ── Fetch Firebase display name ────────────────────────────
    private fun fetchUsername() {
        val user = FirebaseAuth.getInstance().currentUser
        username = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                    ?: ""
    }

    // ── Start a fresh session with a new UUID ──────────────────
    private fun startFreshSession() {
        currentSessionId   = UUID.randomUUID().toString()
        isFirstExchangeDone = false

        val greeting = if (username.isNotBlank())
            "Hello $username! I'm Plutus, your trading psychology coach. " +
                    "You can chat with me, upload a trade journal, or ask about " +
                    "your trading patterns. How can I help you today?"
        else
            "Hello! I'm Plutus, your trading psychology coach. " +
                    "You can chat with me, upload a trade journal, or ask about " +
                    "your trading patterns. How can I help you today?"

        val welcome = ChatMessage(
            id     = UUID.randomUUID().toString(),
            sender = MessageSender.AI,
            text   = greeting
        )
        _uiState.update {
            it.copy(
                messages         = listOf(welcome),
                isTyping         = false,
                hasInsight       = false,
                insightMessage   = "",
                sessionDate      = today(),
                sessionTitle     = "New Chat",
                currentSessionId = currentSessionId
            )
        }
    }

    // ── Public: update last detected pattern ──────────────────
    fun updateLastDetectedPattern(pattern: String) {
        if (pattern.isNotBlank()) lastDetectedPattern = pattern
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
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

                val response = RetrofitClient.api.chatWithPlutus(
                    ChatRequest(
                        traderId    = uid,
                        message     = trimmed,
                        messages    = conversationHistory.toList(),
                        lastPattern = lastDetectedPattern,
                        sessionId   = currentSessionId,
                        username    = username
                    )
                )

                if (response.isSuccessful) {
                    val body      = response.body()
                    val replyText = body?.response ?: "I didn't catch that — could you rephrase?"
                    val title     = body?.title ?: ""

                    conversationHistory.add(
                        mapOf("role" to "assistant", "content" to replyText)
                    )

                    // Bound history to 40 messages
                    if (conversationHistory.size > 40) {
                        val excess = conversationHistory.size - 40
                        repeat(excess) { conversationHistory.removeAt(0) }
                    }

                    val aiMessage = ChatMessage(
                        id     = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text   = replyText
                    )

                    // Update session title if returned on first exchange
                    val updatedTitle = if (title.isNotBlank() && !isFirstExchangeDone) {
                        isFirstExchangeDone = true
                        title
                    } else {
                        _uiState.value.sessionTitle
                    }

                    _uiState.update {
                        it.copy(
                            messages       = it.messages + aiMessage,
                            isTyping       = false,
                            hasInsight     = true,
                            insightMessage = "Plutus has analyzed your trading psychology.",
                            sessionTitle   = updatedTitle
                        )
                    }

                } else {
                    conversationHistory.removeLastOrNull()
                    showError("I'm having trouble connecting right now. Please try again.")
                }

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "sendMessage error: ${e.message}")
                conversationHistory.removeLastOrNull()
                showError("Network error. Please check your connection and try again.")
            }
        }
    }

    // ── Restore a session from history ─────────────────────────
    // Called when user taps a session card in the drawer.
    // Restores messages to the UI and conversation history so the
    // user can continue chatting from exactly where they left off.
    fun restoreSession(session: ChatSession) {
        currentSessionId    = session.sessionId
        isFirstExchangeDone = true   // title already exists

        // Restore conversation history for backend context
        conversationHistory.clear()
        session.messages.forEach { msg ->
            if (msg.role in listOf("user", "assistant") && msg.content.isNotBlank()) {
                conversationHistory.add(mapOf("role" to msg.role, "content" to msg.content))
            }
        }

        // Build UI messages
        val uiMessages = session.messages.mapIndexed { idx, msg ->
            ChatMessage(
                id     = "restored_${session.sessionId}_$idx",
                sender = if (msg.role == "user") MessageSender.USER else MessageSender.AI,
                text   = msg.content
            )
        }

        _uiState.update {
            it.copy(
                messages         = uiMessages,
                isTyping         = false,
                hasInsight       = false,
                insightMessage   = "",
                sessionDate      = session.date.uppercase(),
                sessionTitle     = session.title,
                currentSessionId = session.sessionId
            )
        }
    }

    fun sendQuickAction(action: String) = sendMessage(action)

    // ── New chat ───────────────────────────────────────────────
    fun startNewSession() {
        conversationHistory.clear()
        lastDetectedPattern = ""
        startFreshSession()
    }

    private fun showError(text: String) {
        val errorMsg = ChatMessage(
            id     = UUID.randomUUID().toString(),
            sender = MessageSender.AI,
            text   = text
        )
        _uiState.update {
            it.copy(messages = it.messages + errorMsg, isTyping = false)
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