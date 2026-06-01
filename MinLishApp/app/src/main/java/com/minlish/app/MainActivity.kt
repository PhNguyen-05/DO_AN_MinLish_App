package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minlish.app.feature.auth.AuthViewModel
import com.minlish.app.feature.auth.ForgotPasswordScreen
import com.minlish.app.feature.auth.LoginScreen
import com.minlish.app.feature.auth.RegisterScreen
import com.minlish.app.feature.auth.ResetPasswordScreen
import com.minlish.app.feature.home.HomeScreen
import com.minlish.app.feature.home.HomeViewModel
import com.minlish.app.feature.importexport.ImportExportScreen
import com.minlish.app.feature.importexport.ImportExportViewModel
import com.minlish.app.feature.learning.LearningScreen
import com.minlish.app.feature.learning.LearningViewModel
import com.minlish.app.feature.profile.ProfileScreen
import com.minlish.app.feature.profile.ProfileViewModel
import com.minlish.app.ui.theme.MinLishAppTheme
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MinLishAppTheme {
                MinLishAppNavigation()
            }
        }
    }
}

@Composable
fun MinLishAppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val importExportViewModel: ImportExportViewModel = viewModel()
    val learningViewModel: LearningViewModel = viewModel()

    // Restore login session on app startup for offline auto-login
    val sharedPrefs = context.getSharedPreferences("minlish_auth", android.content.Context.MODE_PRIVATE)
    val savedToken = sharedPrefs.getString("token", null)
    if (savedToken != null && com.minlish.app.data.local.UserSession.token == null) {
        com.minlish.app.data.local.UserSession.token = savedToken
    }

    val startDestination = if (com.minlish.app.data.local.UserSession.token != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgot = { navController.navigate("forgot") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
            HomeScreen(
                viewModel = homeViewModel,
                onProfileClick = { navController.navigate("profile") },
                onImportExportClick = { navController.navigate("import_export") },
                onLearningClick = { deckId, mode ->
                    val deckParam = deckId?.let { "deckId=$it" } ?: ""
                    val modeParam = mode?.let { "mode=$it" } ?: ""
                    val params = listOf(deckParam, modeParam).filter { it.isNotEmpty() }.joinToString("&")
                    val route = if (params.isNotEmpty()) "learning?$params" else "learning"
                    navController.navigate(route)
                }
            )
        }
        composable("import_export") {
            ImportExportScreen(
                viewModel = importExportViewModel,
                onBack = {
                    homeViewModel.fetchDashboardData()
                    navController.popBackStack()
                },
                onStudyDeck = { deckId ->
                    homeViewModel.fetchDashboardData()
                    navController.navigate("learning?deckId=$deckId&mode=mixed")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    homeViewModel.resetState()
                    profileViewModel.resetState()
                    learningViewModel.resetState()
                    navController.navigate("login") { popUpTo("home") { inclusive = true } }
                }
            )
        }
        composable(
            route = "learning?deckId={deckId}&mode={mode}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")?.toLongOrNull()
            val mode = backStackEntry.arguments?.getString("mode")
            LearningScreen(
                viewModel = learningViewModel,
                deckId = deckId,
                initialMode = mode,
                onBack = {
                    homeViewModel.fetchDashboardData()
                    navController.popBackStack()
                }
            )
        }
    }
}
