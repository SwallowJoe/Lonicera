package com.android.lonicera.components.widget

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableContent(
    modifier: Modifier = Modifier,
    title: String,
    onExpandedChange: (Boolean) -> Unit = {},
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    Column(modifier = modifier) {
        OutlinedCard(
            border = ButtonDefaults.outlinedButtonBorder,
            shape = RoundedCornerShape(1.dp),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContentColor = MaterialTheme.colorScheme.secondary,
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                try {
                                    isExpanded = !isExpanded
                                    onExpandedChange(isExpanded)
                                } catch (e: Exception) {
                                    // Handle exception
                                }
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = MaterialTheme.typography.bodyMedium.toSpanStyle()
                                .copy(color = MaterialTheme.colorScheme.primary)
                        ) {
                            append(title)
                        }
                    },
                )
                Icon(
                    imageVector =
                    if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Toggle Icon",
                    tint = Color.Gray,
                )
            }
        }

        // Spacer(modifier = Modifier.height(8.dp))

        if (isExpanded) {
            OutlinedCard(
                border = ButtonDefaults.outlinedButtonBorder,
                shape = RoundedCornerShape(1.dp),
                colors = CardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.secondary,
                )
            ) {
                content()
            }
        }
    }

}

@Preview
@Composable
fun ExpandableContentPreview() {
    ExpandableContent(
        title = "Expandable Content",
        defaultExpanded = true
    ) {
        Text(text = "This is the content that will be displayed when expanded.")
        MenuWithScroll(
            selectedOption = "",
            options = emptyList(),
            onOptionSelected = {
                // Handle option selection
            }
        )
    }
}