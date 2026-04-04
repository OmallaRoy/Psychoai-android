package com.psychoai.app.model.repository


import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.psychoai.app.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = Firebase.firestore

    companion object {
        const val USERS_COLLECTION = "users"
    }

    // Called after registration — creates the user document
    suspend fun createUser(firebaseUser: FirebaseUser, fullName: String): Result<User> {
        return try {
            val user = User(
                uid = firebaseUser.uid,
                fullName = fullName,
                email = firebaseUser.email ?: "",
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                isEmailVerified = false,
                profilePhotoUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
            db.collection(USERS_COLLECTION)
                .document(firebaseUser.uid)
                .set(user)
                .await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Called on every login — updates timestamp
    suspend fun updateLastLogin(uid: String) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .update(
                    mapOf(
                        "lastLoginAt" to System.currentTimeMillis(),
                        "isEmailVerified" to true
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Called after email verification confirmed
    suspend fun updateEmailVerified(uid: String) {
        try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .update("isEmailVerified", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fetch user profile
    suspend fun getUser(uid: String): User? {
        return try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
                .toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Check if document exists (for Google Sign-In)
    suspend fun userExists(uid: String): Boolean {
        return try {
            db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .await()
                .exists()
        } catch (e: Exception) {
            false
        }
    }

    // Google Sign-In — create if new, update if returning
    suspend fun createOrUpdateGoogleUser(firebaseUser: FirebaseUser) {
        try {
            if (userExists(firebaseUser.uid)) {
                updateLastLogin(firebaseUser.uid)
            } else {
                createUser(firebaseUser, firebaseUser.displayName ?: "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}