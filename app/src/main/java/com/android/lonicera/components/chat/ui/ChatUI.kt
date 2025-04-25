package com.android.lonicera.components.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.android.lonicera.R
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.base.StateEffectScaffold
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.widget.AnimatedOverlayDrawerScaffold
import kotlinx.coroutines.delay

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
        initialState = chatViewModel.getInitChatUIState(),
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
                            Text(text = state.chatEntity.title, fontSize = 16.sp)
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
                    LaunchedEffect(
                        state.chatEntity.messages.size,
                        state.chatEntity.updateTimestamp
                    ) {
                        if (state.chatEntity.messages.isNotEmpty()) {
                            if (state.isWaitingResponse) {
                                listState.scrollToItem(state.chatEntity.messages.lastIndex)
                            } else {
                                listState.animateScrollToItem(state.chatEntity.messages.lastIndex)
                            }
                        }
                    }
                    val density = LocalDensity.current
                    val imePadding = WindowInsets.ime.getBottom(density)
                    val isImeVisible = imePadding > 0
                    LaunchedEffect(imePadding) {
                        if (isImeVisible) {
                            if (state.chatEntity.messages.isNotEmpty()) {
                                // listState.animateScrollToItem(state.chatEntity.messages.lastIndex)
                                listState.animateScrollBy(imePadding.toFloat())
                            }
                        }
                    }

                    if (state.isLoading) {
                        Dialog(
                            onDismissRequest = {},
                            properties = DialogProperties(
                                dismissOnBackPress = false,
                                dismissOnClickOutside = false
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = state.isLoading,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(48.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.loading),
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    // 消息列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface),
                        state = listState,
                        contentPadding = PaddingValues(8.dp, 8.dp)
                    ) {
                        items(
                            items = state.chatEntity.messages,
                            key = { message -> message.uuid }
                        ) { message ->
                            ChatBubble(
                                state = state,
                                viewModel = chatViewModel,
                                message = message
                            )
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