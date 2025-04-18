package com.android.lonicera.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.lonicera.components.chat.ui.ChatUI
import com.android.lonicera.components.login.LoginUI
import com.android.lonicera.components.settings.SettingsUI

sealed class Destination(val route: String) {
    data object Login: Destination("login")
    data object Chat: Destination("chat")
    data object Settings: Destination("settings")
    data object Personal: Destination("personal")
    data object About: Destination("about")
}

@Composable
fun NavigatorHost(modifier: Modifier = Modifier,
                  startDestination: Destination = Destination.Login,
                  navHostController: NavHostController) {
    NavHost(
        navController = navHostController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Destination.Login.route) {
            LoginUI(navHostController)
        }
        composable(Destination.Chat.route) {
            ChatUI(navHostController)
        }
        composable(Destination.Settings.route) {
            SettingsUI(navHostController)
        }
    }
}