package com.vahi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vahi.app.R
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.ui.components.WindowClass
import com.vahi.app.ui.components.windowClass
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
 *
 * No `Splash` destination: D11 replaced the old Compose splash screen (a forced
 * wait behind an ExoPlayer video) with the Android system SplashScreen API,
 * installed in `MainActivity` before `setContent` — it isn't part of the nav
 * graph because it isn't Compose content, it's a system window shown during
 * process cold-start and dismissed automatically on first frame.
 */

/** Declared but currently unreachable: docs/22-decisions.md D10 removes the auth
 *  wall from the startup path (launch → library directly). This destination stays
 *  in the graph, un-navigated-to, until `VAHI-406` gives it a Settings entry point —
 *  deleting it now would throw away real UI work for a screen the product still
 *  wants, just not on the critical path. */
@Serializable private data object Auth

@Serializable private data object Library

/** `notebookId` added in Session 6 alongside the persistence layer — see the KDoc
 *  on `CanvasScreen`'s `notebookId` parameter for why it's threaded through even
 *  though nothing inside the editor uses it yet. */
@Serializable private data class Editor(val notebookId: String, val canvasType: PaperStyle)

@Composable
fun AppRoot() {
    // App-shell-level size gate (VAHI-006): checked once, above the nav graph, so
    // every destination gets tablet-only protection — not just the one screen
    // (Auth) that used to check it before the auth wall came off the startup path.
    if (windowClass() != WindowClass.Expanded) {
        TabletOnlyFallback()
        return
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Library) {
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

/** Tablet-only for v1 — decision D2. Shown for Compact/Medium before any route,
 *  including Splash, so a phone user is never shown even one frame of tablet UI. */
@Composable
private fun TabletOnlyFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.tablet_only_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.tablet_only_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
