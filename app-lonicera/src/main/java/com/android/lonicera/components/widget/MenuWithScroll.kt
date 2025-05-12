package com.android.lonicera.components.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.lonicera.R

@Composable
fun MenuWithScroll(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
) {
    var showPopup by remember { mutableStateOf(false) }
    AnchoredPopup(
        onPopupDismissRequested = {
            showPopup = false
        },
        spacing = 4.dp,
        direction = Direction.TOP,
        showPopup = showPopup,
        anchorContent = { modifier ->
            Box(
                modifier = modifier.pointerInput(Unit) {
                    detectTapGestures {
                        showPopup = !showPopup
                    }
                }.background(
                    color = if (!showPopup) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
                ).fillMaxSize()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(4.dp).fillMaxSize(),
                ) {
                    Text(
                        text = selectedOption,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!showPopup) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.align(alignment = Alignment.CenterVertically)
                            .padding(start = 4.dp),
                    )
                    Icon(
                        painter = painterResource(R.drawable.unfold_more_48px),
                        contentDescription = stringResource(R.string.more),
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { modifier ->
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .wrapContentSize()
                .sizeIn(maxWidth = 120.dp, maxHeight = 200.dp),
        ) {
            Column(
                modifier = Modifier
            ) {
                options.forEachIndexed { index, option ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 32.dp, max = 48.dp)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = rememberRipple(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    bounded = true
                                ),
                                onClick = {
                                    showPopup = false
                                    onOptionSelected(option)
                                }
                            )
                    ) {
                        Image(
                            painter = painterResource(R.drawable.page_info_48px),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        )
                    }

                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun MenuWithScrollPreview() {
    Box(modifier = Modifier.size(400.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .sizeIn(
                    minWidth = 0.dp,
                    minHeight = 32.dp,
                    maxWidth = 120.dp,
                    maxHeight = 32.dp
                )
            // .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            MenuWithScroll(
                selectedOption = "Option 1",
                options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5"),
                onOptionSelected = {}
            )
        }
    }
}