package com.android.lonicera.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsUI(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "SettingsUI",
            modifier = Modifier.fillMaxSize()
                .padding(start = 160.dp, top = 320.dp)
        )
    }
}