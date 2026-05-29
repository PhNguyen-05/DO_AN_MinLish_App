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
import com.minlish.app.feature.auth.LoginScreen
import com.minlish.app.feature.auth.RegisterScreen
import com.minlish.app.feature.home.HomeScreen
import com.minlish.app.feature.home.HomeViewModel

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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
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
        composable("home") {
            HomeScreen(viewModel = homeViewModel)
        }
    }
}