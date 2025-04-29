package com.android.lonicera.components.chat.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.lonicera.R
import com.android.lonicera.base.DefaultCoroutineDispatcherProvider
import com.android.lonicera.base.StateEffectScaffold
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.widget.ExpandableContent
import com.android.lonicera.components.widget.MenuWithScroll
import com.llmsdk.base.ChatModel
import kotlin.math.pow

@Composable
fun ChatSettings(state: ChatUIState, viewModel: ChatViewModel, onDismissRequest: () -> Unit) {
    val configuration = LocalConfiguration.current
    val application = LocalContext.current.applicationContext
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val apiKey = remember { mutableStateOf(state.chatConfig.apiKey) }

    Dialog(
        onDismissRequest = {
            viewModel.sendAction(
                ChatUIAction.SetApiKey(
                    model = state.chatConfig.model,
                    apiKey = apiKey.value
                )
            )
            onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    maxWidth = screenWidth - 64.dp,
                    maxHeight = screenHeight - 64.dp
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.chat_settings),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = state.chatEntity.title,
                    onValueChange = {
                        viewModel.sendAction(ChatUIAction.SetTitle(it))
                    },
                    textStyle = TextStyle(fontSize = 12.sp),
                    singleLine = true,
                    readOnly = false,
                    label = {
                        Text(
                            text = stringResource(R.string.title),
                            fontSize = 10.sp
                        )
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.chatEntity.systemPrompt,
                    onValueChange = {
                        viewModel.sendAction(ChatUIAction.SetSystemPrompt(it))
                    },
                    textStyle = TextStyle(fontSize = 12.sp),
                    readOnly = false,
                    label = {
                        Text(
                            text = stringResource(R.string.system_prompt),
                            fontSize = 10.sp
                        )
                    },
                    singleLine = false,
                    maxLines = 12,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
                ExpandableContent(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.model_settings)
                ) {
                    if (state.chatConfig.model.provider == ChatModel.DEEPSEEK_CHAT.provider) {
                        DeepSeekSettings(
                            state = state,
                            viewModel = viewModel,
                            currentApiKey = apiKey.value
                        ) {
                            apiKey.value = it
                        }
                    }
                }
                ExpandableContent(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.chat_display_settings)
                ) {
                    ChatDisplaySettings(
                        state = state,
                        viewModel = viewModel
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(
                        modifier = Modifier.weight(3f)
                    )
                    ClickableText(
                        modifier = Modifier.weight(1f),
                        text = buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.bodyMedium.toSpanStyle()
                                    .copy(color = MaterialTheme.colorScheme.primary)
                            ) {
                                append(stringResource(R.string.cancel))
                            }
                        },
                        onClick = {
                            /*viewModel.sendAction(
                                ChatUIAction.SetApiKey(
                                    model = state.model,
                                    apiKey = apiKey.value
                                )
                            )*/
                            onDismissRequest()
                        }
                    )
                    ClickableText(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f),
                        text = buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.bodyMedium.toSpanStyle()
                                    .copy(color = MaterialTheme.colorScheme.primary)
                            ) {
                                append(stringResource(R.string.save))
                            }
                        },
                        onClick = {
                            viewModel.sendAction(
                                ChatUIAction.SetApiKey(
                                    model = state.chatConfig.model,
                                    apiKey = apiKey.value
                                )
                            )
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChatSettingsPreview() {
    val chatRepository = ChatRepository()
    val chatViewModel = ChatViewModel(
        resources = LocalContext.current.resources,
        chatRepository = chatRepository,
        dispatcherProvider = DefaultCoroutineDispatcherProvider(),
    )
    chatViewModel.sendAction(ChatUIAction.LoadChat)
    StateEffectScaffold(
        viewModel = chatViewModel,
        initialState = ChatUIState(
            // model = chatRepository.getDefaultChatModel(),
            chatEntity = chatRepository.newMessageEntity("new", "test prompt")
        ),
        sideEffect = { _, _ -> }
    ) { viewModel, state ->
        ChatSettings(state, viewModel) {

        }
    }
}

@Composable
private fun DeepSeekSettings(state: ChatUIState,
                             viewModel: ChatViewModel,
                             currentApiKey: String,
                             onApiKeyChanged: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )
        Text(
            text = stringResource(R.string.model_provider),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        MenuWithScroll(
            selectedOption = state.chatConfig.model.nickName,
            options = state.supportedModels.map { it.nickName },
            onOptionSelected = {
                viewModel.sendAction(ChatUIAction.ChangeModel(it))
            }
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = currentApiKey,
            onValueChange = onApiKeyChanged,
            textStyle = TextStyle(
                fontSize = 12.sp,
            ),
            readOnly = false,
            label = {
                Text(
                    text = "API Key",
                    fontSize = 10.sp
                )
            },
            singleLine = true,
            maxLines = 1,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) R.drawable.visibility_48px
                            else R.drawable.visibility_off_48px
                        ),
                        contentDescription = "Toggle password visibility",
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                .background(MaterialTheme.colorScheme.primary),
            thickness = 1.dp
        )

        Text(
            text = stringResource(R.string.context_limit),
            fontSize = 12.sp
        )
        var tokenExponent by remember { mutableFloatStateOf(11.0f) }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Slider(
                value = tokenExponent,
                valueRange = 0f..13f,
                onValueChange = {
                    tokenExponent = it
                    val tokens = 2.0.pow(tokenExponent.toDouble())
                    viewModel.sendAction(ChatUIAction.SetMaxTokens(tokens.toInt()))
                },
                modifier = Modifier.weight(8f)
            )
            Text(
                text = "${state.chatConfig.max_tokens} tokens",
                modifier = Modifier
                    .weight(2f)
                    .padding(start = 8.dp)
            )
        }

        Text(
            text = stringResource(R.string.temperature),
            fontSize = 12.sp
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Slider(
                value = state.chatConfig.temperature.toFloat(),
                valueRange = 0f..2f,
                onValueChange = {
                    viewModel.sendAction(ChatUIAction.SetTemperature(it))
                },
                modifier = Modifier.weight(8f)
            )
            Text(
                // text取state.config.temperature的小数点位保留两位
                text = "%.2f".format(state.chatConfig.temperature),
                modifier = Modifier
                    .weight(2f)
                    .padding(start = 8.dp)
            )
        }

        Text(
            text = "Top P",
            fontSize = 12.sp
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Slider(
                value = state.chatConfig.top_p.toFloat(),
                valueRange = 0f..1f,
                onValueChange = {
                    viewModel.sendAction(ChatUIAction.SetTopP(it))
                },
                modifier = Modifier.weight(8f)
            )
            Text(
                // text取state.config.temperature的小数点位保留两位
                text = "%.2f".format(state.chatConfig.top_p),
                modifier = Modifier
                    .weight(2f)
                    .padding(start = 8.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = state.chatConfig.stream,
                onCheckedChange = {
                    viewModel.sendAction(ChatUIAction.SwitchStreamingState)
                },
                modifier = Modifier.scale(scale = 0.6f)
            )
            Text(
                text = stringResource(R.string.stream_option),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ChatDisplaySettings(
    state: ChatUIState,
    viewModel: ChatViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = state.showTokenCount,
                onCheckedChange = {
                    viewModel.sendAction(ChatUIAction.SwitchShowTokenConsume)
                },
                modifier = Modifier.scale(scale= 0.6f)
            )
            Text(
                text = stringResource(R.string.show_message_token_consume),
                fontSize = 14.sp
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = state.showMessageTimestamp,
                onCheckedChange = {
                    viewModel.sendAction(ChatUIAction.SwitchShowMessageTimestamp)
                },
                modifier = Modifier.scale(scale = 0.6f)
            )
            Text(
                text = stringResource(R.string.show_message_timestamp),
                fontSize = 14.sp
            )
        }
    }
}