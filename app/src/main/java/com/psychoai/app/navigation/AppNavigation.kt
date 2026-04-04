package com.psychoai.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.psychoai.app.ui.screens.auth.EmailVerificationScreen
import com.psychoai.app.ui.screens.auth.ForgotPasswordScreen
import com.psychoai.app.ui.screens.auth.LoginScreen
import com.psychoai.app.ui.screens.auth.RegisterScreen
import com.psychoai.app.ui.screens.auth.ResetPasswordScreen
import com.psychoai.app.ui.screens.chat.ChatScreen
import com.psychoai.app.viewmodel.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = if (authState.isLoggedIn) Screen.Chat.route else Screen.Login.route

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate(Screen.Chat.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.EmailVerification.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EmailVerification.route) {
            EmailVerificationScreen(
                authViewModel = authViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onVerified = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(
                authViewModel = authViewModel,
                onResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                authViewModel = authViewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
