package com.vahi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enum representing the type of notebook canvas.
 */
enum class NotebookType {
    BLANK,
    RULED,
    GRID
}

/**
 * Main notebook canvas screen that renders the appropriate canvas based on type.
 */
@Composable
fun NotebookCanvasScreen(
    notebookType: NotebookType,
    onBack: () -> Unit
) {
    // Handle system back button
    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Canvas content based on type
        when (notebookType) {
            NotebookType.BLANK -> BlankCanvas()
            NotebookType.RULED -> RuledCanvas()
            NotebookType.GRID -> GridCanvas()
        }

        // Back button (top-left)
        BackArrowButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
    }
}

/**
 * Back arrow button component.
 */
@Composable
private fun BackArrowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "←",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
    }
}
