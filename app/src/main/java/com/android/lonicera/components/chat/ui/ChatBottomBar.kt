package com.android.lonicera.components.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.lonicera.R
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.widget.MenuWithScroll

@Composable
fun ChatBottomBar(state: ChatUIState, viewModel: ChatViewModel) {
    var messageText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

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
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp)),
            placeholder = { Text(stringResource(R.string.input_message)) },
            singleLine = false,
            maxLines = 10 // 最大行数
        )

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(modifier = Modifier.size(32.dp),
                onClick = {
                    expanded = !expanded
                }
            ) {
                Icon(
                    imageVector = if (!expanded) Icons.Default.Add else Icons.Default.Close,
                    contentDescription = stringResource(R.string.more),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(modifier = Modifier.size(32.dp),
                onClick = {
                    viewModel.sendAction(ChatUIAction.SwitchReasonableState)
                },
                colors = IconButtonColors(
                    containerColor = if (state.enableReasonable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    contentColor = if (state.enableReasonable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.psychology_48px),
                    contentDescription = stringResource(R.string.more),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(modifier = Modifier.size(32.dp),
                onClick = {
                    viewModel.sendAction(ChatUIAction.SwitchNetworkState)
                },
                colors = IconButtonColors(
                    containerColor = if (state.enableNetwork) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    contentColor = if (state.enableNetwork) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.captive_portal_48px),
                    contentDescription = stringResource(R.string.more),
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .sizeIn(
                        minWidth = 0.dp,
                        minHeight = 32.dp,
                        maxWidth = 120.dp,
                        maxHeight = 32.dp
                    )
                    .align(alignment = Alignment.CenterVertically)
                    // .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                MenuWithScroll(
                    options = state.supportedModels,
                    onOptionSelected = {
                        viewModel.sendAction(ChatUIAction.ChangeModel(it))
                    }
                )
            }

            IconButton(
                modifier = Modifier.size(32.dp),
                enabled = !state.isWaitingResponse,
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
                    contentDescription = stringResource(R.string.send)
                )
            }
        }

        if (expanded) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                ) {

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
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
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                        text = "拍照识别文字",
                        fontSize = 10.sp,
                        color = Color.Gray,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
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
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                        text = "图片识别文字",
                        fontSize = 10.sp,
                        color = Color.Gray,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 12.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
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
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp),
                        text = "文件",
                        fontSize = 10.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(viewModel: ChatViewModel,
                  modelList: List<String>) {
    val selectedOption = remember { mutableStateOf(modelList.first()) }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding()
        ) {
            modelList.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        selectedOption.value = item
                        expanded = false
                        viewModel.sendAction(ChatUIAction.ChangeModel(item))
                    }
                )
            }
        }
        TextField(
            value = selectedOption.value,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }
}