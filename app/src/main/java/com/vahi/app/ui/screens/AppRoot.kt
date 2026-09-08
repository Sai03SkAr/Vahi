package com.vahi.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahi.app.core.model.PaperStyle
import kotlinx.serialization.Serializable

/**
 * Typed navigation-compose routes.
 *
 * Replaces the former hand-rolled `AppDestination` enum + `when` block, and — the
 * more important fix — the `selectedNotebookType` side channel: a second piece of
 * navigation state that had to be kept in sync with `destination` by hand. If it was
 * ever null while `destination == NOTEBOOK_CANVAS`, the screen silently rendered
 * nothing (docs/09-project-status.md bug #8). A typed route argument makes that
 * class of bug impossible: [Editor] cannot exist on the back stack without its
 * [PaperStyle] and notebook ID.
 *
 * This also gives the app a real back stack and, via `rememberNavController`'s
 * built-in `SavedStateHandle` integration, restores the current destination and its
 * arguments across process death — not just simple recomposition. It does not by
 * itself fix bug #1 (rotation losing canvas state): CanvasState itself is still
 * `remember`-scoped inside CanvasScreen, not hoisted into a ViewModel. That is
 * VAHI-110/VAHI-301 territory — see docs/18-architecture-target.md §4.
 */
@Serializable private data object Splash

@Serializable private data object Auth

@Serializable private data object Library

/** `notebookId` added in Session 6 alongside the persistence layer — see the KDoc
 *  on `CanvasScreen`'s `notebookId` parameter for why it's threaded through even
 *  though nothing inside the editor uses it yet. */
@Serializable private data class Editor(val notebookId: String, val canvasType: PaperStyle)

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Splash) {
        composable<Splash> {
            SplashScreen(
                onTimeout = {
                    navController.navigate(Auth) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<Auth> {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Library) {
                        popUpTo(Auth) { inclusive = true }
                    }
                }
            )
        }

        composable<Library> {
            HomeScreen(
                onNavigateToCanvas = { notebookId, paperStyle ->
                    navController.navigate(Editor(notebookId, paperStyle))
                }
            )
        }

        composable<Editor> { backStackEntry ->
            val editor: Editor = backStackEntry.toRoute()
            CanvasScreen(
                notebookId = editor.notebookId,
                canvasType = editor.canvasType,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
