package com.ho.lolive.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ho.lolive.presentation.detail.LiveDetailScreen
import com.ho.lolive.presentation.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail"
}

@Composable
fun LoliveNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onRoomClick = { roomId ->
                    navController.navigate("${Routes.DETAIL}/${Uri.encode(roomId)}")
                },
            )
        }

        composable(
            route = "${Routes.DETAIL}/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
        ) {
            LiveDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
