package com.android.lonicera.components.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Popup
import androidx.navigation.NavHostController
import com.android.lonicera.R
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.base.StateEffectScaffold
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.tool.autoDismissKeyboard
import com.android.lonicera.components.widget.AnimatedOverlayDrawerScaffold
import com.llmsdk.base.ChatModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUI(navHostController: NavHostController, chatViewModel: ChatViewModel) {
    var showChatSettings by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    StateEffectScaffold(
        viewModel = chatViewModel,
        initialState = chatViewModel.getInitChatUIState(),
        sideEffect = { viewModel, sideEffect -> }
    ) { viewModel, state ->
        val drawerState = remember { mutableStateOf(false) }
        AnimatedOverlayDrawerScaffold(
            modifier = Modifier.autoDismissKeyboard(),
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
                            Text(
                                text = state.chatEntity.title,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                drawerState.value = true
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.menu_48dp),
                                    contentDescription = stringResource(R.string.chat_history),
                                    tint =  MaterialTheme.colorScheme.onSurface,
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
                                    tint =  MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
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

                LaunchedEffect(state.chatConfig.apiKey) {
                    if (state.chatConfig.apiKey.isEmpty()) {
                        delay(1000)
                        showApiKeyDialog = true
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
                                listState.scrollToItem(
                                    index = state.chatEntity.messages.lastIndex,
                                    scrollOffset = Int.MAX_VALUE)
                            } else {
                                listState.animateScrollToItem(
                                    index = state.chatEntity.messages.lastIndex,
                                    scrollOffset = Int.MAX_VALUE)
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

                    Popup {
                        AnimatedVisibility(
                            visible = state.isLoading,
                            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 350))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.45f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text(
                                    text = stringResource(R.string.loading),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                                    modifier = Modifier.padding(top = 8.dp)
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
                            // 仅显示非Tool Call和非Tool Response消息
                            items = state.chatEntity.filterMessages { !it.isToolCall && !it.isToolResponse },
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

                if (showApiKeyDialog) {
                    ApiKeyDialog(
                        model = state.chatConfig.model,
                        initialApiKey = state.chatConfig.apiKey,
                        onConfirm = { apiKey ->
                            showApiKeyDialog = false
                            viewModel.sendAction(ChatUIAction.SetApiKey(state.chatConfig.model, apiKey))
                        },
                        onCancel = {
                            showApiKeyDialog = false
                            // 设置空字符串，表示用户取消输入
                            viewModel.sendAction(ChatUIAction.SetApiKey(state.chatConfig.model, " "))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(
    model: ChatModel,
    initialApiKey: String = "",
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "当前大模型(${model.nickName})需要设置ApiKey:",
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontStyle = MaterialTheme.typography.titleMedium.fontStyle,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("请输入ApiKey") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(apiKey) }) {
                Text(
                    text = stringResource(R.string.confirm),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.cancel),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    fontStyle = MaterialTheme.typography.bodyMedium.fontStyle,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}