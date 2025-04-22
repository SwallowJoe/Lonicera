package com.android.lonicera.components.widget

import android.graphics.Path
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.zIndex
import kotlin.math.abs

enum class Direction {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

@Composable
fun AnimatedOverlayDrawerScaffold(
    modifier: Modifier = Modifier,
    drawerContent: @Composable () -> Unit,
    gestureEnabled: Boolean = true,
    direction: Direction = Direction.LEFT,
    showDrawer: MutableState<Boolean> = remember { mutableStateOf(false) },
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var drawerSize by remember { mutableStateOf(IntSize.Zero) } // 用于存储测量的drawer大小

    val maxDrawerOffset = with(density) {
        if (direction == Direction.LEFT || direction == Direction.RIGHT)
            drawerSize.width.toDp()
        else
            drawerSize.height.toDp()
    }

    val dragAmountOffset = remember { mutableStateOf(0.dp) }
    val gestureModifier = Modifier.pointerInput(Unit) {
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (dragAmountOffset.value * 2 > with(density) { drawerSize.width.toDp() }) {
                        showDrawer.value = true
                        dragAmountOffset.value = with(density) { drawerSize.width.toDp() }
                    } else {
                        dragAmountOffset.value = 0.dp
                    }
                }
            ) { change, dragAmount ->
                dragAmountOffset.value += with(density) { dragAmount.toDp() }
                if (abs(dragAmountOffset.value.toPx()) >= drawerSize.width)
                    showDrawer.value = true

                change.consume()
            }
        } else {
            detectVerticalDragGestures(
                onDragEnd = {
                    if (dragAmountOffset.value * 2 > with(density) { drawerSize.height.toDp() }) {
                        showDrawer.value = true
                        dragAmountOffset.value = with(density) { drawerSize.height.toDp() }
                    } else {
                        dragAmountOffset.value = 0.dp
                    }
                }
            ) { change, dragAmount ->
                if (!showDrawer.value /*&& change.position.y < gestureEdgePx && dragAmount > 30*/) {
                    if ((dragAmount > 0 && direction == Direction.TOP)
                        || (dragAmount < 0 && direction == Direction.BOTTOM)
                    ) {
                        dragAmountOffset.value += with(density) { dragAmount.toDp() }
                        if (dragAmountOffset.value >= with(density) { drawerSize.height.toDp() })
                            showDrawer.value = true
                        change.consume()
                    }
                }
            }
        }
    }

    val drawerOffsetState by animateDpAsState(
        targetValue = if (showDrawer.value) maxDrawerOffset else dragAmountOffset.value,
        label = "DrawerOffset"
    )

    val progress = with(LocalDensity.current) {
        val maxPx = maxDrawerOffset.toPx()
        if (maxPx > 0f) {
            drawerOffsetState.toPx() / maxPx
        } else {
            0f // 或 fallback 默认值
        }
    }.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (gestureEnabled) gestureModifier else Modifier)
            .clipToBounds()
    ) {
        val offsetX = when (direction) {
            Direction.LEFT -> drawerOffsetState
            Direction.RIGHT -> -drawerOffsetState
            else -> 0.dp
        }

        val offsetY = when (direction) {
            Direction.TOP -> drawerOffsetState
            Direction.BOTTOM -> -drawerOffsetState
            else -> 0.dp
        }

        // 主内容层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .absoluteOffset(x = offsetX, y = offsetY)
        ) {
            content()

            if (offsetX != 0.dp || offsetY != 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(Color.Black.copy(alpha = 0.5f * progress))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    showDrawer.value = false
                                    dragAmountOffset.value = 0.dp
                                }
                            )
                        }
                )
            }
        }

        // Drawer 内容层
        val drawerModifier = Modifier
            .then(
                when (direction) {
                    Direction.LEFT, Direction.RIGHT -> Modifier
                        .fillMaxHeight()
                        //.width(maxDrawerOffset)
                        .absoluteOffset(x = if (direction == Direction.LEFT)
                            - maxDrawerOffset + offsetX else -offsetX)

                    Direction.TOP, Direction.BOTTOM -> Modifier
                        .fillMaxWidth()
                        //.height(maxDrawerOffset)
                        .absoluteOffset(y = if (direction == Direction.TOP)
                            - maxDrawerOffset + offsetY else -offsetY)
                }
            )
            .onGloballyPositioned { coordinates ->
                drawerSize = coordinates.size
            }

        Box(modifier = drawerModifier) {
            drawerContent()
        }
    }
}