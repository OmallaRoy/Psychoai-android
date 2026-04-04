package com.psychoai.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.psychoai.app.R

@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var resendCooldown by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        authViewModel.clearError()
    }

    // Cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // Poll for verification every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            authViewModel.checkEmailVerification(onVerified)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp, start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }

        // Center content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Modal card
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .padding(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email icon
                    Image(
                        painter = painterResource(id = R.drawable.ic_email),
                        contentDescription = "Google email",
                        modifier = Modifier.size(45.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Check your email",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "We have sent a verification link to your email address. Click the link in the email to activate your account.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Success/error messages
                    authState.successMessage?.let { msg ->
                        SuccessMessage(message = msg)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    authState.error?.let { error ->
                        ErrorMessage(message = error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Resend button
                    PsychoButton(
                        text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend verification email",
                        onClick = {
                            authViewModel.resendVerificationEmail()
                            resendCooldown = 60
                        },
                        isLoading = authState.isLoading,
                        enabled = resendCooldown == 0
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Check now button
                    OutlinedButton(
                        onClick = { authViewModel.checkEmailVerification(onVerified) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Purple
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Purple)
                    ) {
                        Text(text = "I've verified my email", color = Purple)
                    }
                }
            }
        }
    }
}
