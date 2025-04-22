package com.android.lonicera.components.chat.ui

import android.icu.text.SimpleDateFormat
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.android.lonicera.components.chat.model.ChatUIState
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ChatBubble(state: ChatUIState, message: ChatUIMessage) {
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    var showMenu by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) } // 记录触摸点坐标
    val interactionSource = remember { MutableInteractionSource() }
    var currentPress by remember { mutableStateOf<PressInteraction.Press?>(null) }

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
                onLongPress = { offset: Offset ->
                    currentPress?.let {
                        showMenu = true
                        touchOffset = offset
                    }
                }
            )
        },
        horizontalArrangement = if (message.isSender)
                                    Arrangement.End
                                else
                                    Arrangement.Start
    ) {
        if (!message.isSender) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(vertical = 2.dp),
            horizontalAlignment =   if (message.isSender)
                                        Alignment.End
                                    else
                                        Alignment.Start
        ) {
            Row {
                if (!message.isSender) {
                    Image(
                        imageVector = Icons.Default.Person,
                        contentDescription = "头像",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (message.isSender) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(12.dp)
                ) {
                    MarkdownText(
                        markdown = message.content.content
                    )
                }
            }
            var hint = ""
            if (state.showMessageTimestamp) {
                hint += SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(message.timestamp))
            }
            if (!message.isSender && state.showTokenCount) {
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

        if (message.isSender) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (showMenu) {
            val density = LocalDensity.current
            val maxXPx = with(density) { configuration.screenWidthDp.dp.roundToPx() - 200.dp.roundToPx() }
            val adjustedX = touchOffset.x
                .toInt()
                .coerceIn(0, maxXPx) // 限制菜单不超出右边界
                .minus(50.dp.value) // 水平居中补偿
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = {
                    showMenu = false
                },
                offset = with(density) {
                    DpOffset(
                        x = adjustedX.toDp(),
                        y = (touchOffset.y.toInt()).toDp()
                    )
                },
                modifier = Modifier.animateContentSize()
            ) {
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = {
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        showMenu = false
                    }
                )
            }
        }
    }
}