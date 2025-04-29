package com.android.lonicera.components.chat.ui

import android.icu.text.SimpleDateFormat
import android.view.MotionEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.lonicera.R
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.ChatMessage
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatBubble(state: ChatUIState, viewModel: ChatViewModel, message: ChatUIMessage) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    var showMenu by remember { mutableStateOf(false) }
    var lastRawPressPosition by remember { mutableStateOf(Offset.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    var currentPress by remember { mutableStateOf<PressInteraction.Press?>(null) }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(showMenu) {
        if (!showMenu) {
            currentPress?.let { press ->
                scope.launch {
                    interactionSource.emit(
                        PressInteraction.Cancel(press))
                    interactionSource.emit(
                        PressInteraction.Release(press))
                }
            }
        }
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .indication(
            interactionSource = interactionSource,
            indication = rememberRipple())
        .pointerInteropFilter { event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                lastRawPressPosition = Offset(event.x, event.y)
            }
            false
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    currentPress = press
                    interactionSource.emit(press)
                    try {
                        if (tryAwaitRelease()) {
                            // 正常点击处理
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    } finally {
                        if (!showMenu) {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                },
                onLongPress = { _: Offset ->
                    currentPress?.let {
                        showMenu = true
                    }
                }
            )
        },
        horizontalArrangement = if (message.fromUser())
                                    Arrangement.End
                                else
                                    Arrangement.Start
    ) {
        if (!message.fromUser()) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(vertical = 2.dp),
            horizontalAlignment =   if (message.fromUser())
                                        Alignment.End
                                    else
                                        Alignment.Start
        ) {
            Row {
                if (!message.fromUser()) {
                    Image(
                        painter = painterResource(message.avatar),
                        contentDescription = "头像",
                        modifier = Modifier.padding(end = 8.dp).size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (message.fromUser()) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(12.dp)
                ) {
                    var content = ""
                    if (message.message is AssistantMessage && message.message.reasoning_content?.isNotEmpty() == true) {
                        content += "> "
                        content += message.message.reasoning_content?.replace(Regex("\\R")) {
                            "${it.value}> "
                        }
                        content += "\n\n"
                    }
                    if (message.message.content?.isNotEmpty() == true) {
                        content += message.message.content
                    }
                    MarkdownText(
                        markdown = content
                    )
                    if (showMenu) {
                        val density = LocalDensity.current
                        val offset = with(density) {
                            DpOffset(
                                x = lastRawPressPosition.x.toDp()
                                    .coerceIn(0.dp, (configuration.screenWidthDp.dp - 200.dp)),
                                y = (lastRawPressPosition.y - WindowInsets.systemBars.getTop(this)).toDp()
                                    .coerceIn(0.dp, (configuration.screenHeightDp.dp - 200.dp))
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            },
                            offset = offset,
                            modifier = Modifier.animateContentSize()
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.copy)) },
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.message.content?:""))
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    viewModel.sendAction(ChatUIAction.DeleteChat(message))
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
            var hint = ""
            if (state.showMessageTimestamp) {
                hint += SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(message.timestamp))
            }
            if (!message.fromUser() && state.showTokenCount) {
                val totalTokens = message.prompt_hit_tokens + message.prompt_miss_tokens + message.reasoning_tokens + message.completion_tokens
                hint += " | chat ${message.completion_tokens} tokens | prompt hit ${message.prompt_hit_tokens} tokes, prompt miss ${message.prompt_miss_tokens}"
                if (message.reasoning_tokens > 0) {
                    hint += " | reasoning ${message.reasoning_tokens} tokens"
                }
                hint += " | total $totalTokens tokens"
            }
            if (hint.isNotEmpty()) {
                Text(
                    // 展示时间，包含日期和时间
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (message.fromUser()) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}