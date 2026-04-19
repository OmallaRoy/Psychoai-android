//package com.psychoai.app.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.firebase.auth.FirebaseAuth
//import com.psychoai.app.api.ChatRequest
//import com.psychoai.app.api.RetrofitClient
//import com.psychoai.app.model.ChatMessage
//import com.psychoai.app.model.MessageSender
//import com.psychoai.app.model.QuickAction
//import com.psychoai.app.model.TradeTag
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import java.time.LocalDate
//import java.time.format.DateTimeFormatter
//import java.util.UUID
//
//data class ChatUiState(
//    val messages: List<ChatMessage> = emptyList(),
//    val isTyping: Boolean = false,
//    val sessionDate: String = LocalDate.now().format(
//        DateTimeFormatter.ofPattern("MMMM d, yyyy")
//            .withLocale(java.util.Locale.ENGLISH)
//    ).uppercase(),
//    val quickActions: List<QuickAction> = listOf(
//        QuickAction("Fix my risk"),
//        QuickAction("Analyze \$TSLA"),
//        QuickAction("Mindset Check"),
//        QuickAction("Journal")
//    ),
//    val hasInsight: Boolean = false,
//    val insightMessage: String = ""
//)
//
//class ChatViewModel : ViewModel() {
//
//    private val _uiState = MutableStateFlow(ChatUiState())
//    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
//
//    // ── Conversation history list ──────────────────────────────
//    // Stores every user and assistant message for the current session.
//    // Sent with every /chat request so Groq sees full context and
//    // can respond to follow-ups like "yes" correctly instead of
//    // re-introducing itself every time.
//    private val conversationHistory = mutableListOf<Map<String, String>>()
//
//    init {
//        // Welcome message shown when chat opens.
//        // NOT added to conversationHistory — local UI only.
//        val welcome = ChatMessage(
//            id     = UUID.randomUUID().toString(),
//            sender = MessageSender.AI,
//            text   = "Hello! I'm Plutus, your trading psychology coach. " +
//                    "You can chat with me, upload a trade journal " +
//                    "(CSV or Excel), or ask about your trading patterns. " +
//                    "How can I help you today?"
//        )
//        _uiState.update { it.copy(messages = listOf(welcome)) }
//    }
//
//    fun sendMessage(text: String) {
//        if (text.isBlank()) return
//        val trimmed = text.trim()
//
//        // Build user message for the UI
//        val userMessage = ChatMessage(
//            id        = UUID.randomUUID().toString(),
//            sender    = MessageSender.USER,
//            text      = trimmed,
//            tradeTags = extractTradeTags(trimmed)
//        )
//
//        // Append user turn to history BEFORE the API call so the
//        // current message is included in the history sent to backend
//        conversationHistory.add(mapOf("role" to "user", "content" to trimmed))
//
//        // Show user message and typing indicator immediately
//        _uiState.update {
//            it.copy(messages = it.messages + userMessage, isTyping = true)
//        }
//
//        viewModelScope.launch {
//            try {
//                val uid = FirebaseAuth.getInstance().currentUser?.uid
//                    ?: "anonymous"
//
//                // FIX: was RetrofitClient.api.sendChat(...)
//                // Renamed to chatWithPlutus to match PsychoaiApiService
//                val response = RetrofitClient.api.chatWithPlutus(
//                    ChatRequest(
//                        traderId = uid,
//                        message  = trimmed,
//                        messages = conversationHistory.toList() // full history
//                    )
//                )
//
//                if (response.isSuccessful) {
//                    val replyText = response.body()?.response
//                        ?: "I didn't catch that — could you rephrase?"
//
//                    // Append Plutus reply to history so on the NEXT
//                    // message Groq sees what was said here
//                    conversationHistory.add(
//                        mapOf("role" to "assistant", "content" to replyText)
//                    )
//
//                    val aiMessage = ChatMessage(
//                        id     = UUID.randomUUID().toString(),
//                        sender = MessageSender.AI,
//                        text   = replyText
//                    )
//
//                    _uiState.update {
//                        it.copy(
//                            messages = it.messages + aiMessage,
//                            isTyping = false
//                        )
//                    }
//
//                } else {
//                    // Backend returned HTTP error — remove the failed
//                    // user message from history to avoid corruption
//                    conversationHistory.removeLastOrNull()
//
//                    val errorMsg = ChatMessage(
//                        id     = UUID.randomUUID().toString(),
//                        sender = MessageSender.AI,
//                        text   = "I'm having trouble connecting right now. " +
//                                "Please try again in a moment."
//                    )
//                    _uiState.update {
//                        it.copy(
//                            messages = it.messages + errorMsg,
//                            isTyping = false
//                        )
//                    }
//                }
//
//            } catch (e: Exception) {
//                android.util.Log.e("ChatViewModel", "sendMessage error: ${e.message}")
//
//                // Remove the failed user message from history
//                conversationHistory.removeLastOrNull()
//
//                val errorMsg = ChatMessage(
//                    id     = UUID.randomUUID().toString(),
//                    sender = MessageSender.AI,
//                    text   = "Network error. Please check your connection " +
//                            "and try again."
//                )
//                _uiState.update {
//                    it.copy(
//                        messages = it.messages + errorMsg,
//                        isTyping = false
//                    )
//                }
//            }
//        }
//    }
//
//    fun sendQuickAction(action: String) {
//        sendMessage(action)
//    }
//
//    // Called from ChatScreen when "New Chat" is tapped in the drawer
//    fun startNewSession() {
//        // Clear history so Groq starts completely fresh
//        conversationHistory.clear()
//
//        val welcome = ChatMessage(
//            id     = UUID.randomUUID().toString(),
//            sender = MessageSender.AI,
//            text   = "Starting a new session. I'm Plutus, your trading " +
//                    "psychology coach. What would you like to work on today?"
//        )
//
//        _uiState.update {
//            it.copy(
//                messages       = listOf(welcome),
//                isTyping       = false,
//                hasInsight     = false,
//                insightMessage = "",
//                sessionDate    = LocalDate.now().format(
//                    DateTimeFormatter.ofPattern("MMMM d, yyyy")
//                        .withLocale(java.util.Locale.ENGLISH)
//                ).uppercase()
//            )
//        }
//    }
//
//    // Extract ticker symbol tags from message text
//    private fun extractTradeTags(text: String): List<TradeTag> {
//        val tags  = mutableListOf<TradeTag>()
//        val regex = Regex("\\\$([A-Z]{1,5})")
//        regex.find(text)?.let {
//            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
//        }
//        return tags
//    }
//}





package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    private val db = FirebaseFirestore.getInstance()

    // ── Conversation history (v1.1 stateless fix) ─────────────
    // Every user and assistant message is stored here and sent
    // with every /chat call so Groq has full context.
    // Fixes: Plutus re-introducing itself after "yes", "go ahead".
    private val conversationHistory = mutableListOf<Map<String, String>>()

    // ── Last detected pattern (v1.2 Tavily context) ───────────
    // Passed to the backend with every /chat call so Tavily can
    // build a more targeted web search query.
    // e.g. "how do I improve?" + lastPattern="Revenge Trading"
    // → backend searches "revenge trading psychology improvement"
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
    // Call this from ChatScreen when a coaching notification arrives
    // or when the latest coaching result is loaded from backend.
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

        // Append user turn to history BEFORE the API call so this
        // message is included in the history sent to the backend.
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
                        traderId     = uid,
                        message      = trimmed,
                        messages     = conversationHistory.toList(),
                        lastPattern  = lastDetectedPattern
                    )
                )

                if (response.isSuccessful) {
                    val replyText = response.body()?.response
                        ?: "I didn't catch that — could you rephrase?"

                    // Append Plutus reply so on the next message
                    // Groq sees what was just said here.
                    conversationHistory.add(
                        mapOf("role" to "assistant", "content" to replyText)
                    )

                    // Keep history bounded to last 40 messages
                    // (20 exchanges) to prevent prompt growing too large.
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

                    // Save to Firestore for history drawer
                    saveChatSession(uid, trimmed, replyText)

                } else {
                    // HTTP error — remove the failed user message
                    // from history to avoid corrupting the context.
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

                // Remove the failed user message from history.
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

    // ── Save chat session to Firestore ─────────────────────────
    // Persists the exchange so it appears in the history drawer.
    private fun saveChatSession(uid: String, userMessage: String, aiResponse: String) {
        viewModelScope.launch {
            try {
                val entry = hashMapOf(
                    "type"             to "chat",
                    "date"             to LocalDate.now().toString(),
                    "timestamp"        to System.currentTimeMillis(),
                    "first_message"    to userMessage.take(120),
                    "ai_snippet"       to aiResponse.take(200),
                    "pattern"          to lastDetectedPattern,
                    "confidence"       to 0.0,
                    "coaching_snippet" to aiResponse.take(200)
                )
                db.collection("chat_sessions")
                    .document(uid)
                    .collection("sessions")
                    .add(entry)
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Failed to save chat session: ${e.message}")
            }
        }
    }

    // ── Quick action chips ─────────────────────────────────────
    fun sendQuickAction(action: String) {
        sendMessage(action)
    }

    // ── New chat / clear session ───────────────────────────────
    // Called from ChatScreen when "New Chat" is tapped in the drawer.
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

    // ── Extract ticker symbol tags from message ────────────────
    private fun extractTradeTags(text: String): List<TradeTag> {
        val tags  = mutableListOf<TradeTag>()
        val regex = Regex("\\\$([A-Z]{1,5})")
        regex.find(text)?.let {
            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
        }
        return tags
    }
}