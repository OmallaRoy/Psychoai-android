package com.psychoai.app.model


data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = false,
    val profilePhotoUrl: String = "",
    val totalSessions: Int = 0,
    val mindsetScore: Int = 0
)