package com.psychoai.app.ui.screens.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel

// Same Web Client ID as LoginScreen — must be the Web application
// OAuth client ID from Firebase Console / Google Cloud Console
private const val WEB_CLIENT_ID = "149894059595-dbnrc0mobfv70pi5tt6rv49bfebmcaal.apps.googleusercontent.com"
private const val TAG = "GoogleSignIn"

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    // ── Google Sign-In launcher ────────────────────────────────
    // Google accounts are already verified so we navigate directly
    // to the success destination, skipping email verification screen
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Register result code: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Register account: ${account.email}")

                val token = account.idToken
                if (token != null) {
                    Log.d(TAG, "idToken obtained, registering with Firebase")
                    // Google accounts are pre-verified — go straight to success
                    authViewModel.signInWithGoogle(token, onRegisterSuccess)
                } else {
                    Log.e(TAG, "idToken is null — Web Client ID may be wrong")
                    authViewModel.setError("Google sign-up failed: token was null")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Register ApiException — status code: ${e.statusCode} message: ${e.message}")
                authViewModel.setError("Google sign-up failed (code ${e.statusCode})")
            }
        } else {
            Log.w(TAG, "Register result not OK — resultCode: ${result.resultCode}")
        }
    }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

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

        PsychoLogo()

        Spacer(modifier = Modifier.height(24.dp))

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

        // ── Google Sign-Up — now fully wired up ────────────────
        GoogleSignInButton(
            onClick = {
                Log.d(TAG, "Google sign-up button clicked")
                googleSignInClient.signOut() // force account picker to show
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            isDark = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(20.dp))

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
                withStyle(SpanStyle(
                    color = Purple,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline
                )) {
                    append("Terms of Service")
                }
                withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) {
                    append(" and ")
                }
                withStyle(SpanStyle(
                    color = Purple,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline
                )) {
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

        authState.error?.let { error ->
            ErrorMessage(message = error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        PsychoButton(
            text = "Register Now",
            onClick = {
                if (!termsAccepted) return@PsychoButton
                authViewModel.register(
                    fullName, email, password, confirmPassword, onRegisterSuccess
                )
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