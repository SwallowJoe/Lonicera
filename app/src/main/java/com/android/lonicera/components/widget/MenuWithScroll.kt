package com.android.lonicera.components.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.lonicera.R

@Composable
fun MenuWithScroll(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures {
                expanded = !expanded
            }
        }.background(
            color = if (!expanded) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ).fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(4.dp).fillMaxSize(),
        ) {
            Text(
                text = selectedOption,
                modifier = Modifier.align(alignment = Alignment.CenterVertically).padding(start = 4.dp),
                fontSize = 12.sp,
            )
            Icon(
                painter = painterResource(R.drawable.unfold_more_48px),
                contentDescription = stringResource(R.string.more),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            scrollState = scrollState,
            modifier = Modifier.heightIn(
                max = 400.dp
            )
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.sizeIn(
                        minWidth = 100.dp,
                        maxWidth = 200.dp,
                        minHeight = 24.dp,
                        maxHeight = 36.dp
                    ),
                    text = {
                        Text(
                            text = option,
                            fontSize = 12.sp
                        )
                    },
                    onClick = {
                        expanded = false
                        onOptionSelected(option)
                    },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                )

                HorizontalDivider(thickness = 1.dp)
            }
        }
    }
}