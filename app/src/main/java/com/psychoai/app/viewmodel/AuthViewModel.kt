package com.psychoai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.psychoai.app.model.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentUser: FirebaseUser? = null,
    val isEmailVerified: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Check current user on init
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.update {
                it.copy(
                    isLoggedIn = true,
                    currentUser = currentUser,
                    isEmailVerified = currentUser.isEmailVerified
                )
            }
        }

        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.update {
                it.copy(
                    isLoggedIn = user != null,
                    currentUser = user,
                    isEmailVerified = user?.isEmailVerified ?: false
                )
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                // Update last login in Firestore
                auth.currentUser?.uid?.let { uid ->
                    userRepository.updateLastLogin(uid)
                }
                _authState.update { it.copy(isLoading = false, error = null) }
                onSuccess()
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = parseFirebaseError(e.message)
                    )
                }
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        when {
            fullName.isBlank() -> {
                _authState.update { it.copy(error = "Please enter your full name") }
                return
            }
            email.isBlank() -> {
                _authState.update { it.copy(error = "Please enter your email") }
                return
            }
            password.isBlank() -> {
                _authState.update { it.copy(error = "Please enter a password") }
                return
            }
            password.length < 4 -> {
                _authState.update { it.copy(error = "Password must be at least 4 characters") }
                return
            }
            password != confirmPassword -> {
                _authState.update { it.copy(error = "Passwords do not match") }
                return
            }
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                // Update display name
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName.trim())
                    .build()
                result.user?.updateProfile(profileUpdates)?.await()
                // Save user to Firestore
                result.user?.let { firebaseUser ->
                    userRepository.createUser(firebaseUser, fullName.trim())
                }
                // Send verification email
                result.user?.sendEmailVerification()?.await()
                _authState.update { it.copy(isLoading = false, error = null) }
                onSuccess()
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = parseFirebaseError(e.message)
                    )
                }
            }
        }
    }
    fun setError(message: String) {
        _authState.update { it.copy(error = message) }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _authState.update { it.copy(error = "Please enter your email address") }
            return
        }
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _authState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Reset link sent! Check your inbox."
                    )
                }
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = parseFirebaseError(e.message)
                    )
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                _authState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Verification email sent!"
                    )
                }
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = parseFirebaseError(e.message)
                    )
                }
            }
        }
    }

    fun checkEmailVerification(onVerified: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                val verified = auth.currentUser?.isEmailVerified ?: false
                if (verified) {
                    _authState.update { it.copy(isEmailVerified = true) }
                    onVerified()
                } else {
                    _authState.update { it.copy(error = "Email not yet verified. Please check your inbox.") }
                }
            } catch (e: Exception) {
                _authState.update { it.copy(error = parseFirebaseError(e.message)) }
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, error = null) }
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _authState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isLoading = false,
                        error = parseFirebaseError(e.message)
                    )
                }
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.update {
            AuthState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _authState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _authState.update { it.copy(successMessage = null) }
    }

    private fun parseFirebaseError(message: String?): String {
        return when {
            message == null -> "An unexpected error occurred"
            message.contains("INVALID_LOGIN_CREDENTIALS") ||
                    message.contains("wrong-password") ||
                    message.contains("user-not-found") -> "Invalid email or password"
            message.contains("email-already-in-use") -> "This email is already registered"
            message.contains("weak-password") -> "Password is too weak"
            message.contains("invalid-email") -> "Please enter a valid email address"
            message.contains("network-request-failed") -> "No internet connection"
            message.contains("too-many-requests") -> "Too many attempts. Please try again later"
            else -> "Authentication failed. Please try again"
        }
    }
}
