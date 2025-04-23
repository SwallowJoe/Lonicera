package com.android.lonicera.components.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel

@Composable
fun ChatDrawerContent(state: ChatUIState,
                      viewModel: ChatViewModel,
                      onDrawerCloseRequest: () -> Unit,
                      navHostController: NavHostController
) {
    Box(
        modifier = Modifier
            .height(240.dp)
            .width(160.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Spacer(
            modifier = Modifier
                .statusBarsPadding()
        )
        Text(
            text = "ChatDrawerContent"
        )
    }

}