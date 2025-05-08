package com.android.lonicera.components.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun AnchoredPopup(
    modifier: Modifier = Modifier,
    anchorContent: @Composable (modifier: Modifier) -> Unit,
    showPopup: Boolean = false,
    direction: Direction = Direction.TOP,
    spacing: Dp = 0.dp,
    popupProperties: PopupProperties = PopupProperties(),
    onPopupDismissRequested: () -> Unit,
    popupContent: @Composable (modifier: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // 计算可用屏幕区域
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }

    var anchorPosition by remember { mutableStateOf(IntOffset.Zero) }
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }
    val spacingPx = with(density) { spacing.roundToPx() }

    val popupAlpha by animateFloatAsState(
        targetValue = if (showPopup && popupSize.width > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "popupAlpha"
    )
    val popupScale by animateFloatAsState(
        targetValue = if (showPopup && popupSize.width > 0) 1f else 0.5f,
        animationSpec = tween(durationMillis = 250),
        label = "popupScale"
    )

    Box(modifier = modifier) {
        // 获取动态更新的窗口嵌入区域
        anchorContent(
            Modifier.onGloballyPositioned { coordinates ->
                // 获取锚点相对于父容器的位置（非屏幕坐标）
                val positionInWindow = coordinates.positionInWindow()
                anchorPosition =
                    IntOffset(positionInWindow.x.roundToInt(), positionInWindow.y.roundToInt())
                anchorSize = coordinates.size
            }
        )

        val adjustedPopupPosition = remember(
            direction, anchorPosition,
            anchorSize, popupSize, screenWidthPx, screenHeightPx,
            WindowInsets.ime, showPopup
        ) {
            // if (!showPopup) return@remember IntOffset.Zero
            if (popupSize.width <= 0) return@remember IntOffset(anchorPosition.x, anchorPosition.y)
            // 核心对齐计算
            val (baseX, baseY) = calculateCenterAlignedPosition(
                direction = direction,
                anchorPos = anchorPosition,
                anchorSize = anchorSize,
                popupSize = popupSize,
                spacing = spacingPx
            )

            // 边界检测与策略处理
            processPosition(
                baseX = baseX,
                baseY = baseY,
                direction = direction,
                anchorPos = anchorPosition,
                anchorSize = anchorSize,
                popupSize = popupSize,
                screenWidth = screenWidthPx,
                screenHeight = screenHeightPx,
                spacing = spacingPx
            )
        }
/*        ALog.d(
            "AnchoredPopup", "adjustedPopupPosition: $adjustedPopupPosition, "
                    + "anchorPosition: $anchorPosition, "
                    + "anchorSize: $anchorSize, "
                    + "popupSize: $popupSize, "
                    + "screenWidthPx: $screenWidthPx, "
                    + "screenHeightPx: $screenHeightPx"
        )*/
        Popup(
            onDismissRequest = onPopupDismissRequested,
            // alignment = Alignment.TopStart,
            properties = popupProperties,
            offset = IntOffset(
                adjustedPopupPosition.x - anchorPosition.x,
                adjustedPopupPosition.y - anchorPosition.y
            )
        ) {
            Box(
                Modifier.onSizeChanged { size ->
                    popupSize = size
                }.alpha(alpha = popupAlpha).graphicsLayer(scaleX = popupScale, scaleY = popupScale)
            ) {
                popupContent(Modifier)
            }
        }
    }
}

// 屏幕边界检测扩展函数
private fun Rect.isWithinScreen(screenWidth: Int, screenHeight: Int) =
    left >= 0 && top >= 0 && right <= screenWidth && bottom <= screenHeight

// 核心对齐算法
private fun calculateCenterAlignedPosition(
    direction: Direction,
    anchorPos: IntOffset,
    anchorSize: IntSize,
    popupSize: IntSize,
    spacing: Int,
): Pair<Int, Int> {
    val anchorEdgeCenter = when (direction) {
        Direction.TOP -> anchorPos to IntOffset(anchorPos.x + anchorSize.width, anchorPos.y)
        Direction.BOTTOM -> IntOffset(anchorPos.x, anchorPos.y + anchorSize.height) to
                IntOffset(anchorPos.x + anchorSize.width, anchorPos.y + anchorSize.height)
        Direction.LEFT -> anchorPos to IntOffset(anchorPos.x, anchorPos.y + anchorSize.height)
        Direction.RIGHT -> IntOffset(anchorPos.x + anchorSize.width, anchorPos.y) to
                IntOffset(anchorPos.x + anchorSize.width, anchorPos.y + anchorSize.height)
    }

    val (basePoint, edgeLine) = anchorEdgeCenter
    return when (direction) {
        Direction.TOP, Direction.BOTTOM -> {
            val centerX = (edgeLine.x + basePoint.x) / 2 - popupSize.width / 2
            val y = if (direction == Direction.TOP)
                basePoint.y - popupSize.height - spacing
            else
                edgeLine.y + spacing
            Pair(centerX, y)
        }
        Direction.LEFT, Direction.RIGHT -> {
            val x = if (direction == Direction.LEFT)
                basePoint.x - popupSize.width - spacing
            else
                edgeLine.x + spacing
            val centerY = (edgeLine.y + basePoint.y) / 2 - popupSize.height / 2
            Pair(x, centerY)
        }
    }
}

// 位置处理流水线
private fun processPosition(
    baseX: Int,
    baseY: Int,
    direction: Direction,
    anchorPos: IntOffset,
    anchorSize: IntSize,
    popupSize: IntSize,
    spacing: Int,
    screenWidth: Int,
    screenHeight: Int,
): IntOffset {
    val baseBounds = Rect(
        baseX.toFloat(),
        baseY.toFloat(),
        (baseX + popupSize.width).toFloat(),
        (baseY + popupSize.height).toFloat()
    )

    val position = when {
        baseBounds.isWithinScreen(screenWidth, screenHeight) ->
            IntOffset(baseX, baseY)

        else -> {
            val reversedDir = direction.reverse()
            val (reversedX, reversedY) = calculateCenterAlignedPosition(
                direction = reversedDir,
                anchorPos = anchorPos,
                anchorSize = anchorSize,
                popupSize = popupSize,
                spacing = spacing,
            )
            val reversedBounds = Rect(
                reversedX.toFloat(),
                reversedY.toFloat(),
                (reversedX + popupSize.width).toFloat(),
                (reversedY + popupSize.height).toFloat()
            )

            if (reversedBounds.isWithinScreen(screenWidth, screenHeight)) {
                IntOffset(reversedX, reversedY)
            } else {
                smartAdjustment(baseX, baseY, popupSize, screenWidth, screenHeight)
            }
        }
    }
/*    ALog.d("AnchoredPopup", "processPosition: "
        + "position: $position, "
        + "baseX: $baseX, "
        + "baseY: $baseY, "
        + "baseInWindow: ${baseBounds.isWithinScreen(screenWidth, screenHeight)}, "
        + "anchorPos: $anchorPos, "
        + "anchorSize: $anchorSize, "
        + "popupSize: $popupSize, "
        + "screenWidth: $screenWidth, "
        + "screenHeight: $screenHeight"
    )*/
    return position
}

// 智能位置调整（保持最大可见区域）
private fun smartAdjustment(
    x: Int,
    y: Int,
    popupSize: IntSize,
    screenWidth: Int,
    screenHeight: Int
): IntOffset {
    val maxX = screenWidth - popupSize.width
    val maxY = screenHeight - popupSize.height

    return IntOffset(
        x = when {
            x < 0 -> 0
            x > maxX -> maxX
            else -> x
        },
        y = when {
            y < 0 -> 0
            y > maxY -> maxY
            else -> y
        }
    )
}

@Preview
@Composable
fun AnchoredPopupPreview() {
    var showPopup by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.size(200.dp, 300.dp)
            .background(Color.Gray)
            .padding(16.dp)
    ) {

        AnchoredPopup(
            showPopup = showPopup,
            direction = Direction.TOP,
            spacing = 8.dp,
            onPopupDismissRequested = { showPopup = false },
            anchorContent = { mod ->
                Button(
                    onClick = { showPopup = true },
                    modifier = mod
                        .size(120.dp, 60.dp)
                        .background(Color.Blue)
                ) {
                    Text("点击我")
                }
            },
            popupContent = {
                Card(
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column {
                        repeat(5) {
                            Text(
                                "我是弹窗内容",
                                modifier = Modifier.padding(3.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}