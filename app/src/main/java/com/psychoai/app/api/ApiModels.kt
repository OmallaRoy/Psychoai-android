//package com.psychoai.app.api
//
//import com.google.gson.annotations.SerializedName
//
//data class TradeData(
//    val session: String,
//    val pair: String,
//    val direction: String,
//    @SerializedName("lot_size")         val lotSize: Double,
//    @SerializedName("entry_price")      val entryPrice: Double,
//    @SerializedName("risk_percentage")  val riskPercentage: Double,
//    @SerializedName("risk_to_reward")   val riskToReward: String,
//    @SerializedName("market_condition") val marketCondition: String,
//    @SerializedName("emotion_before")   val emotionBefore: String,
//    @SerializedName("stop_loss_used")   val stopLossUsed: Boolean,
//    @SerializedName("pre_trade_plan")   val preTradePlan: String,
//    val hour: Int,
//    @SerializedName("day_of_week")      val dayOfWeek: Int,
//    @SerializedName("is_night")         val isNight: Int = 0
//)
//
//data class AnalyzeRequest(
//    @SerializedName("trader_id") val traderId: String,
//    @SerializedName("fcm_token") val fcmToken: String?,
//    val trade: TradeData,
//    val history: List<TradeData>? = null
//)
//
//// ── FIX: Added messages to carry full conversation history ────
//// Without this: every /chat call only sent the current message.
//// Groq had zero context so Plutus re-introduced itself on every
//// follow-up ("yes", "tell me more", etc.)
//// With this: the entire back-and-forth is sent every time so
//// Groq correctly continues the conversation.
//data class ChatRequest(
//    @SerializedName("trader_id") val traderId: String,
//    val message: String,
//    // Full conversation history: list of {"role":"user"/"assistant","content":"..."}
//    // Defaults to empty list — backwards compatible if backend not yet updated
//    val messages: List<Map<String, String>> = emptyList()
//)
//
//data class SaveTokenRequest(
//    @SerializedName("trader_id") val traderId: String,
//    val token: String
//)
//
//data class SimilarTrader(
//    val rank: Int,
//    @SerializedName("trader_id")  val traderId: String,
//    @SerializedName("last_date")  val lastDate: String,
//    @SerializedName("true_label") val trueLabel: String,
//    val similarity: Double
//)
//
//data class AnalyzeResponse(
//    @SerializedName("trader_id")         val traderId: String,
//    @SerializedName("predicted_mistake") val predictedMistake: String,
//    val confidence: Double,
//    @SerializedName("coaching_pending")  val coachingPending: Boolean,
//    @SerializedName("feature_signals")   val featureSignals: List<String>,
//    @SerializedName("similar_traders")   val similarTraders: List<SimilarTrader>,
//    val message: String
//)
//
//data class CoachingResult(
//    val pattern: String,
//    val confidence: Double,
//    val coaching: String,
//    val signals: List<String>,
//    val timestamp: String
//)
//
//data class PatternEntry(
//    val date: String = "",
//    val pattern: String = "",
//    val confidence: Double = 0.0,
//    @SerializedName("coaching_snippet") val coachingSnippet: String = ""
//)
//
//data class TraderProfile(
//    @SerializedName("trader_id") val traderId: String = "",
//    val sessions: Int = 0,
//    val history: List<PatternEntry> = emptyList()
//)
//
//data class ChatResponse(
//    val response: String,
//    @SerializedName("trader_id") val traderId: String
//)
//
//data class UnifiedHistoryEntry(
//    val type: String = "chat",
//    val date: String = "",
//    val timestamp: Long = 0L,
//    val pattern: String = "",
//    val firstMessage: String = "",
//    val snippet: String = "",
//    val confidence: Double = 0.0
//)
//
//// Used by the unified /trader/{id}/history endpoint
//data class HistoryItem(
//    val type:       String = "",
//    val date:       String = "",
//    val timestamp:  String = "",
//    val title:      String = "",
//    val preview:    String = "",
//    val pattern:    String = "",
//    val confidence: Double = 0.0
//)
//
//data class FullHistory(
//    @SerializedName("trader_id") val traderId: String = "",
//    val items: List<HistoryItem> = emptyList()
//)







package com.psychoai.app.api

import com.google.gson.annotations.SerializedName

// ================================================================
// REQUEST MODELS — what the app SENDS to the backend
// ================================================================

data class TradeData(
    val session:                        String,
    val pair:                           String,
    val direction:                      String,
    @SerializedName("lot_size")         val lotSize: Double,
    @SerializedName("entry_price")      val entryPrice: Double,
    @SerializedName("risk_percentage")  val riskPercentage: Double,
    @SerializedName("risk_to_reward")   val riskToReward: String,
    @SerializedName("market_condition") val marketCondition: String,
    @SerializedName("emotion_before")   val emotionBefore: String,
    @SerializedName("stop_loss_used")   val stopLossUsed: Boolean,
    @SerializedName("pre_trade_plan")   val preTradePlan: String,
    val hour:                           Int,
    @SerializedName("day_of_week")      val dayOfWeek: Int,
    @SerializedName("is_night")         val isNight: Int = 0
)

data class AnalyzeRequest(
    @SerializedName("trader_id") val traderId: String,
    @SerializedName("fcm_token") val fcmToken: String?,
    val trade:                           TradeData,
    val history:                         List<TradeData>? = null
)

// ── ChatRequest (v1.1 + v1.2) ─────────────────────────────────
// v1.1: Added messages list.
//   Without this: every /chat call only sent the current message.
//   Groq had zero context so Plutus re-introduced itself on every
//   follow-up ("yes", "tell me more", etc.)
//   With this: the entire back-and-forth is sent every time so
//   Groq correctly continues the conversation.
//
// v1.2: Added last_pattern for Tavily web search context.
//   When the trader asks a follow-up question, the backend uses
//   last_pattern to build a more targeted Tavily search query.
//   e.g. "how do I improve?" + last_pattern="Revenge Trading"
//   → Tavily searches "revenge trading psychology improvement"
//   instead of the generic "how do I improve trading"
data class ChatRequest(
    @SerializedName("trader_id")    val traderId: String,
    val message:                         String,
    // Full conversation history: list of {"role":"user"/"assistant","content":"..."}
    // Defaults to empty list — backwards compatible if backend not yet updated.
    val messages:                        List<Map<String, String>> = emptyList(),
    // Last pattern detected by TCN — helps build better Tavily search queries.
    // Empty string if no journal has been analyzed yet in this session.
    @SerializedName("last_pattern") val lastPattern: String = ""
)

data class SaveTokenRequest(
    @SerializedName("trader_id") val traderId: String,
    val token:                           String
)

// ================================================================
// RESPONSE MODELS — what the app RECEIVES from the backend
// ================================================================

data class SimilarTrader(
    val rank:                            Int,
    @SerializedName("trader_id")  val traderId: String,
    @SerializedName("last_date")  val lastDate: String,
    @SerializedName("true_label") val trueLabel: String,
    val similarity:                      Double
)

data class AnalyzeResponse(
    @SerializedName("trader_id")         val traderId: String,
    @SerializedName("predicted_mistake") val predictedMistake: String,
    val confidence:                      Double,
    @SerializedName("coaching_pending")  val coachingPending: Boolean,
    @SerializedName("feature_signals")   val featureSignals: List<String>,
    @SerializedName("similar_traders")   val similarTraders: List<SimilarTrader>,
    val message:                         String
)

data class CoachingResult(
    val pattern:    String,
    val confidence: Double,
    val coaching:   String,
    val signals:    List<String>,
    val timestamp:  String
)

// ================================================================
// PatternEntry — one past coaching session stored in Firestore.
//
// GET /trader/{id}/profile returns { history: [ PatternEntry, ... ] }
// This is what the hamburger menu history drawer displays.
//
// sessions is Int not Long — Long causes a type mismatch
// compilation error with Kotlin delegate properties.
// ================================================================
data class PatternEntry(
    val date:                              String = "",
    val pattern:                           String = "",
    val confidence:                        Double = 0.0,
    @SerializedName("coaching_snippet") val coachingSnippet: String = ""
)

data class TraderProfile(
    @SerializedName("trader_id") val traderId: String = "",
    val sessions:                        Int = 0,
    val history:                         List<PatternEntry> = emptyList()
)

data class ChatResponse(
    val response:                        String,
    @SerializedName("trader_id") val traderId: String
)

// ================================================================
// UnifiedHistoryEntry — covers both coaching sessions and chat.
// Used by the history drawer to show all past interactions.
//
// type = "coaching" → from journal upload, has pattern badge + emoji
// type = "chat"     → from free conversation, shows first message
//
// timestamp is Long (milliseconds) for chat sessions written by
// ChatViewModel via System.currentTimeMillis(). Coaching sessions
// from the backend store ISO string dates so their timestamp
// defaults to 0L and date string is used for sorting instead.
// ================================================================
data class UnifiedHistoryEntry(
    val type:         String = "chat",
    val date:         String = "",
    val timestamp:    Long   = 0L,
    val pattern:      String = "",
    val firstMessage: String = "",
    val snippet:      String = "",
    val confidence:   Double = 0.0
)

// ================================================================
// HistoryItem / FullHistory — used by GET /trader/{id}/history
// which returns coaching + chat sessions merged and sorted by
// the backend. Alternative to client-side merging.
// ================================================================
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
    val items:                           List<HistoryItem> = emptyList()
)