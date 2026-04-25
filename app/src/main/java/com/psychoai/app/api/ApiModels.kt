package com.psychoai.app.api

import com.google.gson.annotations.SerializedName

// ================================================================
// REQUEST MODELS
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

// v1.5: session_id groups all messages in one conversation into
// a single Firestore doc. username lets Plutus greet by name.
data class ChatRequest(
    @SerializedName("trader_id")    val traderId:    String,
    val message:                               String,
    val messages:                              List<Map<String, String>> = emptyList(),
    @SerializedName("last_pattern") val lastPattern: String = "",
    @SerializedName("session_id")   val sessionId:   String = "",
    val username:                              String = ""
)

data class SaveTokenRequest(
    @SerializedName("trader_id") val traderId: String,
    val token:                           String
)

// ================================================================
// RESPONSE MODELS
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
    val response:                 String,
    @SerializedName("trader_id") val traderId: String,
    // Returned only on first message of a session — used to update drawer
    val title:                    String = ""
)

// One message stored inside a session document
data class SessionMessage(
    val role:      String = "",
    val content:   String = "",
    val timestamp: String = ""
)

// One complete session — one card in the history drawer
data class ChatSession(
    @SerializedName("session_id") val sessionId: String = "",
    @SerializedName("trader_id")  val traderId:  String = "",
    val title:                          String = "Trading Psychology Session",
    val date:                           String = "",
    @SerializedName("created_at")  val createdAt:  String = "",
    @SerializedName("updated_at")  val updatedAt:  String = "",
    val messages:                        List<SessionMessage> = emptyList()
)

data class SessionsResponse(
    @SerializedName("trader_id") val traderId: String = "",
    val sessions:                        List<ChatSession> = emptyList()
)

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