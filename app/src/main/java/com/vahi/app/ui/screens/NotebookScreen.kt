package com.vahi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vahi.app.ui.components.windowClass
import com.vahi.app.ui.components.WindowClass

@Composable
fun NotebookScreen() {
    when (windowClass()) {
        WindowClass.Expanded -> NotebookTabletLayout()
        else -> NotebookFallbackLayout()
    }
}

@Composable
private fun NotebookTabletLayout() {
    Column(modifier = Modifier.fillMaxSize()) {

        // Top toolbar (tools, page controls)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            // Empty toolbar placeholder
        }

        Row(modifier = Modifier.fillMaxSize()) {

            // Canvas area
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Empty canvas placeholder
            }

            // Right panel (AI / research later)
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            ) {
                // Empty placeholder
            }
        }
    }
}

@Composable
private fun NotebookFallbackLayout() {
    Surface(modifier = Modifier.fillMaxSize()) {}
}
