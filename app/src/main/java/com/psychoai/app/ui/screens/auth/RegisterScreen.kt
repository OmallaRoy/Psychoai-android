package com.psychoai.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.clearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // Logo
        PsychoLogo()

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Welcome",
            color = TextPrimary,
            fontSize = 28.sp,
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create an account to start your journey",
            color = TextSecondary,
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Google Sign In
        GoogleSignInButton(
            onClick = { /* Google sign-in */ },
            isDark = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(20.dp))

        // Full Name
        FieldLabel(text = "Full Name")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                authViewModel.clearError()
            },
            placeholder = "Enter your name",
            leadingIcon = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Email
        FieldLabel(text = "Email")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = email,
            onValueChange = {
                email = it
                authViewModel.clearError()
            },
            placeholder = "Enter your email",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password
        FieldLabel(text = "Password")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = password,
            onValueChange = {
                password = it
                authViewModel.clearError()
            },
            placeholder = "Enter your password",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Confirm Password
        FieldLabel(text = "Confirm Password")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                authViewModel.clearError()
            },
            placeholder = "Confirm your password",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terms checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Purple,
                    uncheckedColor = InputBorder,
                    checkmarkColor = TextPrimary
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            val termsText = buildAnnotatedString {
                withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) {
                    append("By registering, you agree to our ")
                }
                withStyle(SpanStyle(color = Purple, fontSize = 13.sp, textDecoration = TextDecoration.Underline)) {
                    append("Terms of Service")
                }
                withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) {
                    append(" and ")
                }
                withStyle(SpanStyle(color = Purple, fontSize = 13.sp, textDecoration = TextDecoration.Underline)) {
                    append("Privacy Policy")
                }
                withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) {
                    append(".")
                }
            }
            Text(
                text = termsText,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error
        authState.error?.let { error ->
            ErrorMessage(message = error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Register Button
        PsychoButton(
            text = "Register Now",
            onClick = {
                if (!termsAccepted) {
                    return@PsychoButton
                }
                authViewModel.register(fullName, email, password, confirmPassword, onRegisterSuccess)
            },
            isLoading = authState.isLoading,
            enabled = termsAccepted
        )

        Spacer(modifier = Modifier.height(28.dp))

        val loginText = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary, fontSize = 14.sp)) {
                append("Have an account? ")
            }
            withStyle(SpanStyle(color = Purple, fontSize = 14.sp)) {
                append("Log in")
            }
        }
        Text(
            text = loginText,
            modifier = Modifier.clickable { onNavigateToLogin() }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
