package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psychoai.app.model.ChatMessage
import com.psychoai.app.model.InsightCard
import com.psychoai.app.model.MessageSender
import com.psychoai.app.model.QuickAction
import com.psychoai.app.model.TradeTag
import kotlinx.coroutines.delay
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
        DateTimeFormatter.ofPattern("MMMM d, yyyy").withLocale(java.util.Locale.ENGLISH)
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

    init {
        // Seed with demo conversation matching the screenshot
        val demoMessages = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.USER,
                text = "I just closed my long position on \$NVDA. I think I entered too late and panicked when it dipped. Can you look at my entry?",
                tradeTags = listOf(
                    TradeTag("SYMBOL", "\$NVDA"),
                    TradeTag("LOSS", "-\$420.50"),
                    TradeTag("HOLD", "14m")
                )
            ),
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI,
                text = "I've analyzed your execution. You entered at the top of a parabolic move. This looks like a classic FOMO response to the morning breakout.",
                insightCard = InsightCard(
                    title = "Revenge Trade Warning",
                    description = "This trade was opened exactly 4 minutes after your stop-loss hit on \$AMD. Psychologically, you were hunting for a 'makeup trade' to erase the previous loss.",
                    actionLabel = "Audit"
                )
            )
        )
        _uiState.update {
            it.copy(
                messages = demoMessages,
                hasInsight = true,
                insightMessage = "We identified 3 recurring pitfalls in your scalping sessions today."
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = text.trim(),
            tradeTags = extractTradeTags(text)
        )

        _uiState.update {
            it.copy(messages = it.messages + userMessage, isTyping = true)
        }

        viewModelScope.launch {
            delay(1200) // simulate thinking
            val aiResponse = generateAiResponse(text, userMessage.tradeTags)
            _uiState.update {
                it.copy(
                    messages = it.messages + aiResponse,
                    isTyping = false
                )
            }
        }
    }

    fun sendQuickAction(action: String) {
        sendMessage(action)
    }

    private fun extractTradeTags(text: String): List<TradeTag> {
        val tags = mutableListOf<TradeTag>()
        val symbolRegex = Regex("\\\$([A-Z]{1,5})")
        symbolRegex.find(text)?.let {
            tags.add(TradeTag("SYMBOL", "\$${it.groupValues[1]}"))
        }
        return tags
    }

    private fun generateAiResponse(input: String, tags: List<TradeTag>): ChatMessage {
        val lowerInput = input.lowercase()

        val (responseText, insight) = when {
            lowerInput.contains("risk") || lowerInput.contains("fix my risk") -> {
                Pair(
                    "Based on your recent trades, you're risking 3.2% per trade — nearly 60% above your stated 2% rule. Your position sizing is emotionally driven, not systematic.",
                    InsightCard(
                        title = "Risk Rule Violation",
                        description = "You've broken your 2% risk rule 4 times this week. Each violation followed a winning streak — classic overconfidence bias.",
                        actionLabel = "Set Rules"
                    )
                )
            }
            lowerInput.contains("tsla") || lowerInput.contains("tesla") -> {
                Pair(
                    "TSLA is showing elevated volatility with a beta of 2.1 right now. Your historical win rate on high-beta momentum plays is 38% — below breakeven for your risk/reward setup.",
                    InsightCard(
                        title = "Pattern Alert",
                        description = "You've traded TSLA 7 times this month. 6 of those entries were during the first 30 minutes — when your emotional reactivity is highest.",
                        actionLabel = "View History"
                    )
                )
            }
            lowerInput.contains("mindset") || lowerInput.contains("mental") -> {
                Pair(
                    "Your mindset score today is 62/100. You're trading with elevated stress markers — three trades were entered within 2 minutes of a loss, signaling revenge trading patterns.",
                    InsightCard(
                        title = "Mindset Report",
                        description = "Take a 15-minute break. Your P&L recovery rate after breaks is 73% higher than when you continue trading through emotional states.",
                        actionLabel = "Start Break Timer"
                    )
                )
            }
            lowerInput.contains("journal") -> {
                Pair(
                    "Here's your trading journal for today. You made 6 trades, had a 50% win rate, but your losses were 2.3x larger than your wins — resulting in a net negative session.",
                    InsightCard(
                        title = "Today's Summary",
                        description = "Key theme: You're cutting winners too early and holding losers too long. The disposition effect is costing you an estimated \$340 per day.",
                        actionLabel = "Full Report"
                    )
                )
            }
            lowerInput.contains("fomo") -> {
                Pair(
                    "FOMO (Fear Of Missing Out) is one of the most common psychological biases in trading. It causes traders to enter positions after the optimal entry has already passed.",
                    InsightCard(
                        title = "FOMO Pattern Detected",
                        description = "Your FOMO trades have a 28% win rate vs your planned trades at 61%. The data is clear: waiting for your setup pays significantly better.",
                        actionLabel = "See FOMO Trades"
                    )
                )
            }
            else -> {
                val symbol = tags.firstOrNull { it.label == "SYMBOL" }?.value
                val symbolText = if (symbol != null) " on $symbol" else ""
                Pair(
                    "I've analyzed your query$symbolText. Let me break down the psychological factors at play in your trading behavior and how we can optimize your decision-making process.",
                    InsightCard(
                        title = "Behavioral Insight Ready",
                        description = "Based on your trading history, I've identified 3 patterns that may be affecting your performance today.",
                        actionLabel = "Audit"
                    )
                )
            }
        }

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.AI,
            text = responseText,
            insightCard = insight
        )
    }
}
