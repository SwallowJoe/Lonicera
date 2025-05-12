package com.android.lonicera.components.settings.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.lonicera.R

data class SettingsOption(
    @DrawableRes val startIcon: Int,
    val title: String,
    val description: String? = null,
    @DrawableRes val endIcon: Int? = null
)

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    options: List<SettingsOption>,
    onOptionSelected: (SettingsOption) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        items(options.size) { index ->
            SettingsOptionItem(
                // modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                option = options[index],
                onOptionSelected = onOptionSelected
            )
        }
    }
}

@Composable
private fun SettingsOptionItem(
    modifier: Modifier = Modifier,
    option: SettingsOption,
    onOptionSelected: (SettingsOption) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 40.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    bounded = true
                ),
                onClick = { onOptionSelected(option) }
            )
    ) {
        Image(
            painter = painterResource(option.startIcon),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(16.dp)
        )
        Text(
            text = option.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        if (option.description?.isNotEmpty() == true) {
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(end = 8.dp)
            )
        }
        if (option.endIcon != null) {
            Image(
                painter = painterResource(option.endIcon),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(12.dp)
            )
        }
    }
}

@Preview
@Composable
fun SettingsItemPreview() {
    val options = ArrayList<SettingsOption>()
    repeat(10) {
        options.add(SettingsOption(
            startIcon = R.drawable.ai_bot,
            title = "Option 1",
        ))
    }
    SettingsItem(
        options = options
    ) {

    }
}