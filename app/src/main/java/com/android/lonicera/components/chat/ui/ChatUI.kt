package com.android.lonicera.components.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.android.lonicera.R
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.base.StateEffectScaffold
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.widget.AnimatedOverlayDrawerScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUI(navHostController: NavHostController) {
    val chatViewModel = ChatViewModel(
        resources = LocalContext.current.resources,
        chatRepository = ChatRepository(),
        dispatcherProvider = DefaultCoroutineDispatcherProvider(),
    )
    chatViewModel.sendAction(ChatUIAction.LoadChat)
    var showChatSettings by remember { mutableStateOf(false) }
    StateEffectScaffold(
        viewModel = chatViewModel,
        initialState = ChatUIState(""),
        sideEffect = { viewModel, sideEffect -> }
    ) { viewModel, state ->
        val drawerState = remember { mutableStateOf(false) }
        AnimatedOverlayDrawerScaffold(
            modifier = Modifier,
            showDrawer = drawerState,
            drawerContent = {
                ChatDrawerContent(
                    state = state,
                    viewModel = chatViewModel,
                    onDrawerCloseRequest = {
                        drawerState.value = false
                    },
                    navHostController = navHostController
                )
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(text = state.title, fontSize = 16.sp)
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                drawerState.value = true
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.sort_48px),
                                    contentDescription = stringResource(R.string.chat_history),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    showChatSettings = !showChatSettings
                                    // navHostController.navigate(Destination.Settings.route)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.settings_48px),
                                    contentDescription = stringResource(R.string.settings),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = Color.Black,
                            navigationIconContentColor = Color.Black,
                            actionIconContentColor = Color.Black
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) { innerPadding ->
                if (showChatSettings) {
                    ChatSettings(state = state, viewModel = chatViewModel) {
                        showChatSettings = false
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(state.messages.size) {
                        if (state.messages.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }
                    // 消息列表
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                        reverseLayout = true, // 最新消息在底部
                        state = listState,
                        contentPadding = PaddingValues(0.dp, 8.dp)
                    ) {
                        items(
                            items = state.messages.reversed(),
                            key = { message -> message.hashCode() }
                        ) { message ->
                            key(message.timestamp) {
                                ChatBubble(
                                    state = state,
                                    viewModel = chatViewModel,
                                    message = message)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(top = 2.dp)
                    ) {
                        ChatBottomBar(state, viewModel)
                    }
                }
            }
        }
    }
}