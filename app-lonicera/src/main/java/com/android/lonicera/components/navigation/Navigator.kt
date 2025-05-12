package com.android.lonicera.components.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.components.about.AboutUI
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.chat.ui.ChatUI
import com.android.lonicera.components.login.LoginUI
import com.android.lonicera.components.modelprovider.ModelProviderUI
import com.android.lonicera.components.settings.ui.SettingsUI
import com.android.lonicera.components.tool.ToolUI

sealed class Destination(val route: String) {
    data object Login: Destination("login")
    data object Chat: Destination("chat")
    data object Settings: Destination("settings")
    data object Personal: Destination("personal")
    data object About: Destination("about")
    data object Tool: Destination("tool")
    data object ModelProvider: Destination("model_provider")
}

@Composable
fun NavigatorHost(modifier: Modifier = Modifier,
                  startDestination: Destination = Destination.Login,
                  navHostController: NavHostController) {
    val chatViewModel = ViewModelProvider.getChatViewModel(
        resources = LocalContext.current.resources,
        chatRepository = ChatRepository(),
        dispatcherProvider = DefaultCoroutineDispatcherProvider(),
    )

    LaunchedEffect(Unit) {
        chatViewModel.sendAction(ChatUIAction.LoadChat)
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(Destination.Login.route) {
            LoginUI(navHostController)
        }
        composable(Destination.Chat.route) {
            ChatUI(navHostController, chatViewModel)
        }
        composable(Destination.Settings.route) {
            SettingsUI(navHostController)
        }
        composable(Destination.About.route) {
            AboutUI(navHostController)
        }
        composable(Destination.Tool.route) {
            ToolUI(navHostController)
        }
        composable(Destination.ModelProvider.route) {
            ModelProviderUI(navHostController)
        }
    }
}