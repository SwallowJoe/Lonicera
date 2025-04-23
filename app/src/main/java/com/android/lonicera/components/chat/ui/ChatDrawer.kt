package com.android.lonicera.components.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.android.lonicera.R
import com.android.lonicera.components.chat.model.ChatUIAction
import com.android.lonicera.components.chat.model.ChatUIState
import com.android.lonicera.components.chat.model.ChatViewModel
import com.android.lonicera.components.navigation.Destination

@Composable
fun ChatDrawerContent(state: ChatUIState,
                      viewModel: ChatViewModel,
                      onDrawerCloseRequest: () -> Unit,
                      navHostController: NavHostController
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Image(
                    painter = painterResource(R.drawable.ai_bot),
                    contentDescription = "AI Bot",
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.typography.titleMedium.color,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                                fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                            )
                        ) {
                            append(stringResource(R.string.app_name))
                        }
                    },
                    modifier = Modifier.padding(start = 10.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { onDrawerCloseRequest() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.chat_history),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // .background(MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.chat_history))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        // Clean history messages.
                        viewModel.sendAction(ChatUIAction.CleanChatHistory)
                    },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.chat_history),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.messageEntities.sortedByDescending { it.timestamp }) { messageEntity ->
                    Box(
                        modifier = Modifier
                            .then(
                                if (messageEntity.id == state.id) {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                                } else Modifier
                            )
                    )

                    Text(
                        text = messageEntity.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    viewModel.sendAction(
                                        ChatUIAction.SelectChat(
                                            messageEntity
                                        )
                                    )
                                },
                                indication = rememberRipple(bounded = true), // 开启涟漪
                                interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                            )
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )

            // New chat
            Row(
                horizontalArrangement = Arrangement.Absolute.Left,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClick = {
                            viewModel.sendAction(ChatUIAction.NewChat)
                        },
                        indication = rememberRipple(bounded = true), // 开启涟漪
                        interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.add_circle_48px),
                    contentDescription = stringResource(R.string.new_chat),
                    modifier = Modifier.padding(start = 24.dp).size(16.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.new_chat))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Tool Settings
            Row(
                horizontalArrangement = Arrangement.Absolute.Left,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClick = {
                            navHostController.navigate(Destination.Tool.route)
                        },
                        indication = rememberRipple(bounded = true), // 开启涟漪
                        interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.build_48px),
                    contentDescription = stringResource(R.string.tool_settings),
                    modifier = Modifier.padding(start = 24.dp).size(16.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.tool_settings))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Model provider info
            Row(
                horizontalArrangement = Arrangement.Absolute.Left,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClick = {
                            navHostController.navigate(Destination.ModelProvider.route)
                        },
                        indication = rememberRipple(bounded = true), // 开启涟漪
                        interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.monitoring_48px),
                    contentDescription = stringResource(R.string.model_provider_info),
                    modifier = Modifier.padding(start = 24.dp).size(16.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.model_provider_info))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Common Settings
            Row(
                horizontalArrangement = Arrangement.Absolute.Left,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClick = {
                            navHostController.navigate(Destination.Settings.route)
                        },
                        indication = rememberRipple(bounded = true), // 开启涟漪
                        interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.settings_48px),
                    contentDescription = stringResource(R.string.settings),
                    modifier = Modifier.padding(start = 24.dp).size(16.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.settings))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // About
            Row(
                horizontalArrangement = Arrangement.Absolute.Left,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClick = {
                            navHostController.navigate(Destination.About.route)
                        },
                        indication = rememberRipple(bounded = true), // 开启涟漪
                        interactionSource = remember { MutableInteractionSource() } // 必须搭配使用
                    )
            ) {
                Image(
                    painter = painterResource(R.drawable.info_48px),
                    contentDescription = stringResource(R.string.about),
                    modifier = Modifier.padding(start = 24.dp).size(16.dp)
                )

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                            )
                        ) {
                            append(stringResource(R.string.about))
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
