package com.android.lonicera.components.tool

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun ToolUI(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Tool",
            modifier = Modifier.fillMaxSize()
                .padding(start = 160.dp, top = 320.dp)
        )
    }
}