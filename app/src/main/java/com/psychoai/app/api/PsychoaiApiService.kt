package com.psychoai.app.api

import retrofit2.Response
import retrofit2.http.*

interface PsychoaiApiService {

    @POST("analyze_trade")
    suspend fun analyzeTrade(
        @Body request: AnalyzeRequest
    ): Response<AnalyzeResponse>

    @GET("coaching/{traderId}/latest")
    suspend fun getLatestCoaching(
        @Path("traderId") traderId: String
    ): Response<CoachingResult>

    @GET("trader/{traderId}/profile")
    suspend fun getTraderProfile(
        @Path("traderId") traderId: String
    ): Response<TraderProfile>

    @GET("trader/{traderId}/history")
    suspend fun getFullHistory(
        @Path("traderId") traderId: String
    ): Response<FullHistory>

    // Returns all chat sessions for the history drawer — one per conversation
    @GET("trader/{traderId}/sessions")
    suspend fun getChatSessions(
        @Path("traderId") traderId: String
    ): Response<SessionsResponse>

    // Returns a single session document — used to restore conversation
    @GET("session/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String
    ): Response<ChatSession>

    @POST("chat")
    suspend fun chatWithPlutus(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    @POST("fcm_token")
    suspend fun saveFcmToken(
        @Body request: SaveTokenRequest
    ): Response<Map<String, String>>

    @GET("health")
    suspend fun health(): Response<Map<String, String>>
}