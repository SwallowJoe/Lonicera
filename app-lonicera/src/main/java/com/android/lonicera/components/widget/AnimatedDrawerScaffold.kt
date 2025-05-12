package com.android.lonicera.components.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedDrawerScaffold(
    modifier: Modifier = Modifier,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var showDrawer by remember { mutableStateOf(false) }

    // 控制 Drawer 宽度动画
    val drawerWidth by animateDpAsState(
        targetValue = if (showDrawer) 240.dp else 0.dp,
        label = "Drawer Width"
    )

    // 手势感应区域宽度（例如左边缘 240.dp 范围可滑动展开）
    val gestureEdgeWidth = 240.dp

    // 将 dp 转 px 以便计算手势偏移量
    val density = LocalDensity.current
    val gestureEdgePx = with(density) { gestureEdgeWidth.toPx() }

    // 手势处理逻辑
    val gestureModifier = Modifier.pointerInput(showDrawer) {
        detectHorizontalDragGestures { change, dragAmount ->
            if (!showDrawer && change.position.x < gestureEdgePx && dragAmount > 30) {
                showDrawer = true
            } else if (showDrawer && dragAmount < -30) {
                showDrawer = false
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .then(gestureModifier) // 加上手势监听
    ) {
        // 左侧 Drawer
        Box(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (showDrawer) {
                drawerContent()
            }
        }

        // 主内容
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}