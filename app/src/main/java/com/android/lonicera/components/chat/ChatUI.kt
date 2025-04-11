package com.android.lonicera.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.lonicera.R
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.base.StateEffectScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUI(navigateBack: () -> Unit) {
    val chatViewModel = ChatViewModel(
        chatRepository = ChatRepository("新对话"),
        dispatcherProvider = DefaultCoroutineDispatcherProvider(),
    )
    chatViewModel.sendAction(ChatUIAction.LoadChat("新对话"))
    var messageText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    StateEffectScaffold(
        viewModel = chatViewModel,
        initialState = ChatUIState.Chat(),
        sideEffect = { viewModel, sideEffect -> }
    ) { viewModel, state ->
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(text = when (state) {
                            is ChatUIState.Empty -> "聊天"
                            is ChatUIState.Loading -> "加载中..."
                            is ChatUIState.Chat -> state.title
                        }, fontSize = 16.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                painter = painterResource(R.drawable.sort_48px),
                                contentDescription = "历史记录",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                // TODO: settings...
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.settings_48px),
                                contentDescription = "设置",
                                modifier = Modifier.size(24.dp))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 消息列表
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true // 最新消息在底部
                ) {
                    when (state) {
                        is ChatUIState.Empty -> {

                        }
                        is ChatUIState.Loading -> {

                        }
                        is ChatUIState.Chat -> {
                            items(state.messages.reversed()) { message ->
                                ChatBubble(message = message)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    // 输入区域
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            placeholder = { Text("输入消息...") },
                            singleLine = false,
                            maxLines = 10 // 最大行数
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(modifier = Modifier.size(32.dp),
                                onClick = {
                                    expanded = !expanded
                                }
                            ) {
                                Icon(
                                    imageVector = if (!expanded) Icons.Default.Add else Icons.Default.Close,
                                    contentDescription = "更多",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            val clickable = state !is ChatUIState.Loading && (state is ChatUIState.Chat && !state.isWaitingResponse)
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                enabled = clickable,
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendAction(ChatUIAction.SendMessage(messageText))
                                        messageText = ""
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                },
                                colors = IconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primary),
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "发送")
                            }
                        }

                        if (expanded) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                                ) {

                                    Button(
                                        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(8.dp)),
                                        onClick = {
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.photo_camera_48px),
                                            contentDescription = "拍照识别文字",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                                        text = "拍照识别文字",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                                ) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(8.dp)),
                                        onClick = {
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.imagesmode_48px),
                                            contentDescription = "图片识别文字",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                                        text = "图片识别文字",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                                ) {
                                    Button(
                                        modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(8.dp)),
                                        onClick = {
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surfaceContainer),
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.38f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.upload_file_48px),
                                            contentDescription = "上传文件",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                                        text = "文件",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}