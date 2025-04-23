package com.android.lonicera.components.widget

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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue

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
    showDrawer: MutableState<Boolean> = mutableStateOf(false),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var drawerSize by remember { mutableStateOf(DpSize.Zero) } // 用于存储测量的drawer大小
    var contentSize by remember { mutableStateOf(DpSize.Zero) }

    val maxDrawerOffset =
        if (direction == Direction.LEFT || direction == Direction.RIGHT)
            drawerSize.width
        else
            drawerSize.height

    var dragAmountOffset by remember { mutableStateOf(0.dp) }

    LaunchedEffect(showDrawer.value) {
        dragAmountOffset = if (showDrawer.value) {
            maxDrawerOffset
        } else {
            0.dp
        }
    }

    val gestureModifier = Modifier.pointerInput(Unit) {
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    dragAmountOffset = if (dragAmountOffset.toPx().absoluteValue * 2 > drawerSize.width.toPx()) {
                        if (direction == Direction.LEFT) drawerSize.width
                        else -drawerSize.width
                    } else {
                        0.dp
                    }
                }
            ) { change, dragAmount ->
                dragAmountOffset += with(density) { dragAmount.toDp() }
                if (dragAmountOffset.toPx().absoluteValue >= drawerSize.width.toPx()) {
                    dragAmountOffset =
                        if (direction == Direction.LEFT) drawerSize.width
                        else -drawerSize.width

                } else if (direction == Direction.LEFT && dragAmountOffset < 0.dp) {
                    dragAmountOffset = 0.dp
                } else if (direction == Direction.RIGHT && dragAmountOffset > 0.dp) {
                    dragAmountOffset = 0.dp
                }

                change.consume()
            }
        } else {
            detectVerticalDragGestures(
                onDragEnd = {
                    dragAmountOffset = if (dragAmountOffset.toPx().absoluteValue * 2 > drawerSize.height.toPx()) {
                        if (direction == Direction.TOP) drawerSize.height
                        else -drawerSize.height
                    } else {
                        0.dp
                    }
                }
            ) { change, dragAmount ->
                dragAmountOffset += with(density) { dragAmount.toDp() }
                if (dragAmountOffset.toPx().absoluteValue >= drawerSize.height.toPx()) {
                    dragAmountOffset =
                        if (direction == Direction.TOP) drawerSize.height
                        else -drawerSize.height
                } else if (direction == Direction.TOP && dragAmountOffset < 0.dp) {
                    dragAmountOffset = 0.dp
                } else if (direction == Direction.BOTTOM && dragAmountOffset > 0.dp) {
                    dragAmountOffset = 0.dp
                }

                change.consume()
            }
        }
    }

    val drawerOffsetState by animateDpAsState(
        targetValue = dragAmountOffset,
        label = "DrawerOffset"
    )

    val progress = with(LocalDensity.current) {
        val maxPx = maxDrawerOffset.toPx().absoluteValue
        if (maxPx > 0f) {
            drawerOffsetState.toPx().absoluteValue / maxPx
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
            Direction.RIGHT -> drawerOffsetState
            else -> 0.dp
        }

        val offsetY = when (direction) {
            Direction.TOP -> drawerOffsetState
            Direction.BOTTOM -> drawerOffsetState
            else -> 0.dp
        }

        // 主内容层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .absoluteOffset(x = offsetX, y = offsetY)
                .onGloballyPositioned { coordinates ->
                    contentSize = with(density) {
                        DpSize(
                            coordinates.size.width.toDp(),
                            coordinates.size.height.toDp()
                        )
                    }
                }
        ) {
            content()

            if (offsetX != 0.dp || offsetY != 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                        .background(Color.Black.copy(alpha = 0.3f * progress))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    dragAmountOffset = 0.dp
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
                        .absoluteOffset(
                            x =
                            if (direction == Direction.LEFT) - maxDrawerOffset + offsetX
                            else contentSize.width + offsetX
                        )

                    Direction.TOP, Direction.BOTTOM -> Modifier
                        .fillMaxWidth()
                        .absoluteOffset(
                            y =
                            if (direction == Direction.TOP) - maxDrawerOffset + offsetY
                            else contentSize.height + offsetY
                        )
                }
            )
            .onGloballyPositioned { coordinates ->
                drawerSize = with(density) {
                    DpSize(
                        coordinates.size.width.toDp(),
                        coordinates.size.height.toDp()
                    )
                }
            }


        Box(modifier = drawerModifier) {
            drawerContent()
        }
    }
}