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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.psychoai.app.ui.components.*
import com.psychoai.app.ui.theme.*
import com.psychoai.app.viewmodel.AuthViewModel

private const val WEB_CLIENT_ID = "149894059595-dbnrc0mobfv70pi5tt6rv49bfebmcaal.apps.googleusercontent.com"
private const val TAG = "GoogleSignIn"

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Google Sign-In launcher with full logging to diagnose issues
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Login result code: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Login account: ${account.email}")

                val token = account.idToken
                if (token != null) {
                    Log.d(TAG, "idToken obtained, signing in with Firebase")
                    authViewModel.signInWithGoogle(token, onLoginSuccess)
                } else {
                    Log.e(TAG, "idToken is null — Web Client ID may be wrong")
                    authViewModel.setError("Google sign-in failed: token was null")
                }
            } catch (e: ApiException) {
                // Status code guide:
                // 10  = Developer error (SHA-1 not in Firebase OR wrong Web Client ID)
                // 12501 = User cancelled
                // 7   = Network error
                // 16  = Internal error
                Log.e(TAG, "Login ApiException — status code: ${e.statusCode} message: ${e.message}")
                authViewModel.setError("Google sign-in failed (code ${e.statusCode})")
            }
        } else {
            Log.w(TAG, "Login result not OK — resultCode: ${result.resultCode}")
        }
    }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    LaunchedEffect(Unit) { authViewModel.clearError() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            PsychoLogo()
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Psycho AI",
                color = Purple,
                fontSize = 26.sp,
                style = MaterialTheme.typography.displayMedium
            )
        }

        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = "Welcome Back",
            color = TextPrimary,
            fontSize = 30.sp,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        GoogleSignInButton(
            onClick = {
                Log.d(TAG, "Google login button clicked")
                googleSignInClient.signOut()
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            isDark = false
        )

        Spacer(modifier = Modifier.height(24.dp))
        OrDivider()
        Spacer(modifier = Modifier.height(24.dp))

        FieldLabel(text = "Email Address")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = email,
            onValueChange = { email = it; authViewModel.clearError() },
            placeholder = "name@example.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        FieldLabel(text = "Password")
        Spacer(modifier = Modifier.height(8.dp))
        PsychoTextField(
            value = password,
            onValueChange = { password = it; authViewModel.clearError() },
            placeholder = "••••••••",
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot Password?",
                color = Purple,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .clickable { onNavigateToForgotPassword() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        authState.error?.let {
            ErrorMessage(message = it)
            Spacer(modifier = Modifier.height(12.dp))
        }

        PsychoButton(
            text = "Login",
            onClick = { authViewModel.login(email, password, onLoginSuccess) },
            isLoading = authState.isLoading
        )

        Spacer(modifier = Modifier.height(32.dp))

        val annotatedText = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary, fontSize = 14.sp)) {
                append("Don't have an account? ")
            }
            withStyle(SpanStyle(color = Purple, fontSize = 14.sp)) {
                append("Sign up")
            }
        }
        Text(
            text = annotatedText,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}