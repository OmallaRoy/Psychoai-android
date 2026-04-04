package com.psychoai.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object EmailVerification : Screen("email_verification")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password")
    object Chat : Screen("chat")
}
