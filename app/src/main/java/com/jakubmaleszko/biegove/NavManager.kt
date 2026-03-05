package com.jakubmaleszko.biegove
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jakubmaleszko.biegove.pages.MainPage
import com.jakubmaleszko.biegove.pages.TablePage
import kotlinx.serialization.Serializable


sealed interface Screen {
    @Serializable
    data object Main : Screen

    @Serializable
    data object  Table : Screen
}

@Composable
fun NavManager() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Main
    ) {
        // Define the Main Screen
        composable<Screen.Main> {
            MainPage(onNavigateToTable = {navController.navigate(Screen.Table)})
        }
        composable<Screen.Table> {
            TablePage(onBack = {navController.popBackStack()})
        }
    }
}