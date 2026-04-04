package com.psychoai.app.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class MessageSender { USER, AI }

data class TradeTag(
    val label: String,
    val value: String
)

data class InsightCard(
    val title: String,
    val description: String,
    val actionLabel: String? = null
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val tradeTags: List<TradeTag> = emptyList(),
    val insightCard: InsightCard? = null,
    val isLoading: Boolean = false
)

data class QuickAction(
    val label: String
)
