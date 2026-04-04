package com.psychoai.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ResetPasswordScreen(
    authViewModel: AuthViewModel,
    onResetSuccess: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Password",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                FieldLabel(text = "New Password")
                Spacer(modifier = Modifier.height(8.dp))
                PsychoTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = null
                    },
                    placeholder = "Enter your new password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                FieldLabel(text = "Confirm Password")
                Spacer(modifier = Modifier.height(8.dp))
                PsychoTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    placeholder = "Confirm your new password",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                error?.let { err ->
                    ErrorMessage(message = err)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                PsychoButton(
                    text = "Reset Password",
                    onClick = {
                        when {
                            newPassword.isBlank() -> error = "Please enter a new password"
                            newPassword.length < 6 -> error = "Password must be at least 6 characters"
                            newPassword != confirmPassword -> error = "Passwords do not match"
                            else -> {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        Firebase.auth.currentUser?.updatePassword(newPassword)?.await()
                                        onResetSuccess()
                                    } catch (e: Exception) {
                                        error = "Failed to reset password. Please try again."
                                    }
                                    isLoading = false
                                }
                            }
                        }
                    },
                    isLoading = isLoading
                )
            }
        }
    }
}
