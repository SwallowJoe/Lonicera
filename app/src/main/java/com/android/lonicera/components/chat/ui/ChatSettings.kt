package com.android.lonicera.components.chat.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ChatSettings(onDismissRequest: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier.sizeIn(
                    maxWidth = screenWidth - 64.dp,
                    maxHeight = screenHeight - 64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text("Chat Settings")
                OutlinedTextField(
                    value = "HHH",
                    onValueChange = {},
                    singleLine = true,
                    readOnly = false,
                    label = { Text("Title") },
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = "HHH\nKKK\nMMM\nLLL\nZZZZ",
                    onValueChange = {

                    },
                    readOnly = false,
                    label = { Text("Prompt") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ChatSettingsPreview() {
    ChatSettings {

    }
}