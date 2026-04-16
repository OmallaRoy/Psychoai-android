package com.psychoai.app.api

import com.google.gson.annotations.SerializedName

data class TradeData(
    val session: String,
    val pair: String,
    val direction: String,
    @SerializedName("lot_size")         val lotSize: Double,
    @SerializedName("entry_price")      val entryPrice: Double,
    @SerializedName("risk_percentage")  val riskPercentage: Double,
    @SerializedName("risk_to_reward")   val riskToReward: String,
    @SerializedName("market_condition") val marketCondition: String,
    @SerializedName("emotion_before")   val emotionBefore: String,
    @SerializedName("stop_loss_used")   val stopLossUsed: Boolean,
    @SerializedName("pre_trade_plan")   val preTradePlan: String,
    val hour: Int,
    @SerializedName("day_of_week")      val dayOfWeek: Int,
    @SerializedName("is_night")         val isNight: Int = 0
)

data class AnalyzeRequest(
    @SerializedName("trader_id") val traderId: String,
    @SerializedName("fcm_token") val fcmToken: String?,
    val trade: TradeData,
    val history: List<TradeData>? = null
)

// ── FIX: Added messages to carry full conversation history ────
// Without this: every /chat call only sent the current message.
// Groq had zero context so Plutus re-introduced itself on every
// follow-up ("yes", "tell me more", etc.)
// With this: the entire back-and-forth is sent every time so
// Groq correctly continues the conversation.
data class ChatRequest(
    @SerializedName("trader_id") val traderId: String,
    val message: String,
    // Full conversation history: list of {"role":"user"/"assistant","content":"..."}
    // Defaults to empty list — backwards compatible if backend not yet updated
    val messages: List<Map<String, String>> = emptyList()
)

data class SaveTokenRequest(
    @SerializedName("trader_id") val traderId: String,
    val token: String
)

data class SimilarTrader(
    val rank: Int,
    @SerializedName("trader_id")  val traderId: String,
    @SerializedName("last_date")  val lastDate: String,
    @SerializedName("true_label") val trueLabel: String,
    val similarity: Double
)

data class AnalyzeResponse(
    @SerializedName("trader_id")         val traderId: String,
    @SerializedName("predicted_mistake") val predictedMistake: String,
    val confidence: Double,
    @SerializedName("coaching_pending")  val coachingPending: Boolean,
    @SerializedName("feature_signals")   val featureSignals: List<String>,
    @SerializedName("similar_traders")   val similarTraders: List<SimilarTrader>,
    val message: String
)

data class CoachingResult(
    val pattern: String,
    val confidence: Double,
    val coaching: String,
    val signals: List<String>,
    val timestamp: String
)

data class PatternEntry(
    val date: String = "",
    val pattern: String = "",
    val confidence: Double = 0.0,
    @SerializedName("coaching_snippet") val coachingSnippet: String = ""
)

data class TraderProfile(
    @SerializedName("trader_id") val traderId: String = "",
    val sessions: Int = 0,
    val history: List<PatternEntry> = emptyList()
)

data class ChatResponse(
    val response: String,
    @SerializedName("trader_id") val traderId: String
)

data class UnifiedHistoryEntry(
    val type: String = "chat",
    val date: String = "",
    val timestamp: Long = 0L,
    val pattern: String = "",
    val firstMessage: String = "",
    val snippet: String = "",
    val confidence: Double = 0.0
)

// Used by the unified /trader/{id}/history endpoint
data class HistoryItem(
    val type:       String = "",
    val date:       String = "",
    val timestamp:  String = "",
    val title:      String = "",
    val preview:    String = "",
    val pattern:    String = "",
    val confidence: Double = 0.0
)

data class FullHistory(
    @SerializedName("trader_id") val traderId: String = "",
    val items: List<HistoryItem> = emptyList()
)