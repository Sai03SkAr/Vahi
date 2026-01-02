package com.vahi.app.ui.screens

import androidx.compose.runtime.*

@Composable
fun AppRoot() {

    var destination by remember {
        mutableStateOf(AppDestination.SPLASH)
    }

    // Track the notebook type for canvas navigation
    var selectedNotebookType by remember {
        mutableStateOf<NotebookType?>(null)
    }

    when (destination) {

        AppDestination.SPLASH -> {
            SplashScreen(
                onTimeout = {
                    destination = AppDestination.AUTH
                }
            )
        }

        AppDestination.AUTH -> {
            AuthScreen(
                onLoginSuccess = {
                    destination = AppDestination.HOME
                }
            )
        }

        AppDestination.HOME -> {
            HomeScreen(
                onNavigateToCanvas = { notebookType ->
                    selectedNotebookType = notebookType
                    destination = AppDestination.NOTEBOOK_CANVAS
                }
            )
        }

        AppDestination.NOTEBOOK_CANVAS -> {
            selectedNotebookType?.let { type ->
                NotebookCanvasScreen(
                    notebookType = type,
                    onBack = {
                        destination = AppDestination.HOME
                    }
                )
            }
        }
    }
}
