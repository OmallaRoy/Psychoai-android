package com.psychoai.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var resendCooldown by remember { mutableStateOf(0) }
    var emailSent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.clearError()
        authViewModel.clearSuccessMessage()
    }

    // Track when email is sent
    LaunchedEffect(authState.successMessage) {
        if (authState.successMessage != null) {
            emailSent = true
            resendCooldown = 60
        }
    }

    // Cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Forgot Password",
                color = TextPrimary,
                fontSize = 17.sp,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        HorizontalDivider(color = SurfaceMedium, thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Forgot Your Password?",
                color = TextPrimary,
                fontSize = 26.sp,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enter your registered email to receive a reset link.",
                color = TextSecondary,
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            PsychoTextField(
                value = email,
                onValueChange = {
                    email = it
                    authViewModel.clearError()
                },
                placeholder = "Email address",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error/Success messages
            authState.error?.let { error ->
                ErrorMessage(message = error)
                Spacer(modifier = Modifier.height(12.dp))
            }

            authState.successMessage?.let { msg ->
                SuccessMessage(message = msg)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Send reset link button
            PsychoButton(
                text = "Send reset link",
                onClick = {
                    authViewModel.sendPasswordResetEmail(email)
                },
                isLoading = authState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info texts shown after sending
            if (emailSent) {
                Text(
                    text = "A reset link will be sent to your registered email address",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "This link is valid for 10 minutes. Please check your inbox.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Didn't receive the link? ",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (resendCooldown > 0) {
                        Text(
                            text = "Resend in ${resendCooldown}s",
                            color = TextHint,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "Resend",
                            color = Purple,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                authViewModel.sendPasswordResetEmail(email)
                                resendCooldown = 60
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Back to login
            Text(
                text = "Back to Log In",
                color = Purple,
                fontSize = 15.sp,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}
