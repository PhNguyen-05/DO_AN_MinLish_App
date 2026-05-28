package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minlish.app.feature.auth.AuthViewModel
import com.minlish.app.feature.auth.ForgotPasswordScreen
import com.minlish.app.feature.auth.LoginScreen
import com.minlish.app.feature.auth.RegisterScreen
import com.minlish.app.feature.auth.ResetPasswordScreen
import com.minlish.app.feature.home.HomeScreen
import com.minlish.app.feature.home.HomeViewModel
import com.minlish.app.feature.profile.ProfileScreen
import com.minlish.app.feature.profile.ProfileViewModel
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinLishAppNavigation()
        }
    }
}

@Composable
fun MinLishAppNavigation() {
    val navController = rememberNavController()

    // Cấp phát các bộ não xử lý (ViewModel) cho các Feature tương ứng
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgot = { navController.navigate("forgot") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true } // Clear stack tránh back ngược lại Login
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") }
            )
        }
        composable("forgot") {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateToReset = { email ->
                    val encoded = URLEncoder.encode(email, "UTF-8")
                    navController.navigate("reset/$encoded")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("reset/{email}") { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("email")
            val email = encoded?.let { URLDecoder.decode(it, "UTF-8") }
            ResetPasswordScreen(
                viewModel = authViewModel,
                email = email,
                onResetSuccess = { navController.navigate("login") { popUpTo("login") { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }
        composable("home") {
            HomeScreen(viewModel = homeViewModel, onProfileClick = { navController.navigate("profile") })
        }
        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }
    }
}