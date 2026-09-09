package com.vahi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.vahi.app.R
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.Notebook
import com.vahi.app.ui.feature.library.LibraryViewModel
import com.vahi.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/* ---------- DATA ---------- */
// `Notebook` used to be a display-only data class declared right here, with two
// hardcoded sample instances. It's now the real, persisted domain model —
// see domain/model/Notebook.kt.

data class CardMenuAnchor(
    val notebookId: String,
    val offset: Offset
)

/** New notebooks rotate through this palette rather than all sharing one colour —
 *  there's no colour-picker UI yet (doc 15 §4.5's fuller create-modal design), so
 *  this is the cheapest way new cards don't look identical.
 *
 *  Stored as ARGB ints because that's what the domain model carries — `Color` is a
 *  presentation type and stops at this layer (docs/18-architecture-target.md §3). */
private val notebookColorPalette = listOf(
    0xFFBABEFF.toInt(), 0xFFD3C3C3.toInt(), 0xFFC4E8D1.toInt(), 0xFFFFE0B2.toInt(), 0xFFB2D8FF.toInt()
)

/* ---------- SCREEN ---------- */

@Composable
fun HomeScreen(
    onNavigateToCanvas: (notebookId: String, paperStyle: PaperStyle) -> Unit = { _, _ -> },
    onNavigateToSettings: () -> Unit = {}
) {
    // hiltViewModel() here is the hilt-navigation-compose:1.3.0 API, deprecated in
    // favour of a new androidx.hilt.lifecycle.viewmodel.compose package — that
    // package ships in the SAME artifact whose 1.4.0 release requires compileSdk 37
    // / AGP 9.1+ (see gradle/libs.versions.toml's hiltNavigationCompose comment).
    // Chasing the deprecation would reintroduce the exact conflict staying on 1.3.0
    // avoids, for a warning with no functional cost. Left as-is deliberately.
    val viewModel: LibraryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Density for converting captured pointer/layout offsets (px) to dp for overlay positioning —
    // see docs/09, bug #3: these offsets come from positionInRoot() and are in pixels, not dp.
    val density = LocalDensity.current

    // State for full-screen modal
    var isCreateNotebookOpen by remember { mutableStateOf(false) }

    // State for dropdown overlays
    var cardMenuAnchor by remember { mutableStateOf<CardMenuAnchor?>(null) }
    var isProfileOpen by remember { mutableStateOf(false) }

    // "All" vs "Pinned notes" — filtered client-side from the already-loaded list.
    // NotebookRepository.observePinned() exists for a future dedicated surface but
    // isn't used here; see docs/21-backlog.md VAHI-207.
    var showPinnedOnly by remember { mutableStateOf(false) }

    // Title search (VAHI-408) — same client-side-filter pattern as showPinnedOnly
    // above, for the same reason: the dataset is small and already fully loaded,
    // so there's nothing a server-side or ViewModel-level query would buy here.
    var searchQuery by remember { mutableStateOf("") }

    // Button positions for anchoring overlays
    var profileButtonPosition by remember { mutableStateOf(Offset.Zero) }

    // Helper functions for overlay management
    fun openCardMenu(anchor: CardMenuAnchor) {
        isProfileOpen = false
        cardMenuAnchor = anchor
    }

    fun openProfile() {
        cardMenuAnchor = null
        isProfileOpen = true
    }

    fun closeDropdowns() {
        cardMenuAnchor = null
        isProfileOpen = false
    }

    fun openCreateNotebook() {
        closeDropdowns()
        isCreateNotebookOpen = true
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModel.deleteNotebook(notebook.id)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "\"${notebook.title}\" deleted",
                actionLabel = "Undo",
                withDismissAction = false,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreNotebook(notebook.id)
            }
        }
    }

    // Determine if any dropdown is open
    val isAnyDropdownOpen = cardMenuAnchor != null || isProfileOpen

    // Handle back button for all overlays
    BackHandler(enabled = isCreateNotebookOpen || isAnyDropdownOpen) {
        if (isCreateNotebookOpen) {
            isCreateNotebookOpen = false
        } else {
            closeDropdowns()
        }
    }

    val visibleNotebooks = uiState.notebooks
        .let { notebooks -> if (showPinnedOnly) notebooks.filter { it.isPinned } else notebooks }
        .let { notebooks ->
            if (searchQuery.isBlank()) notebooks
            else notebooks.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: Main Content (with blur when Create Notebook modal is open)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isCreateNotebookOpen) Modifier.blur(8.dp)
                    else Modifier
                )
        ) {
            HomeContent(
                notebooks = visibleNotebooks,
                showPinnedOnly = showPinnedOnly,
                onFilterChanged = { showPinnedOnly = it },
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                onCreateNotebook = { openCreateNotebook() },
                onOpenNotebook = { notebook -> onNavigateToCanvas(notebook.id, notebook.paperStyle) },
                onSettings = onNavigateToSettings,
                onProfile = { openProfile() },
                onCardMenuOpen = { anchor -> openCardMenu(anchor) },
                onProfileButtonPositioned = { position -> profileButtonPosition = position }
            )
        }

        // Layer 2: Dropdown Overlays (Card Menu, Settings, Profile)
        if (isAnyDropdownOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeDropdowns() }
            ) {
                // Card Context Menu - anchored to the three-dot button, positioned within screen
                cardMenuAnchor?.let { anchor ->
                    val menuNotebook = uiState.notebooks.find { it.id == anchor.notebookId }
                    val topPadding = with(density) { anchor.offset.y.toDp() } + 40.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding, end = 24.dp)
                    ) {
                        if (menuNotebook != null) {
                            CardContextMenu(
                                isPinned = menuNotebook.isPinned,
                                onDismiss = { closeDropdowns() },
                                onDelete = { deleteNotebook(menuNotebook) },
                                onTogglePin = { viewModel.togglePin(menuNotebook.id, !menuNotebook.isPinned) }
                            )
                        }
                    }
                }

                // Profile Overlay - anchored below Profile avatar, positioned within screen
                if (isProfileOpen) {
                    val topPadding = with(density) { profileButtonPosition.y.toDp() } + 48.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding, end = 24.dp)
                    ) {
                        ProfileOverlay(onDismiss = { closeDropdowns() })
                    }
                }
            }
        }

        // Layer 3: Full-screen Create Notebook Modal (highest priority)
        if (isCreateNotebookOpen) {
            Box(modifier = Modifier.zIndex(20f)) {
                CreateNotebookOverlay(
                    defaultPaperStyle = uiState.defaultPaperStyle,
                    onDismiss = { isCreateNotebookOpen = false },
                    onOptionSelected = { option ->
                        isCreateNotebookOpen = false
                        // Typed dispatch (VAHI-211) — this used to compare against the
                        // "Blank"/"Ruled"/"Grid" display strings, so renaming a label in
                        // the UI would silently break notebook creation.
                        when (option) {
                            is CreateNotebookChoice.Paper -> coroutineScope.launch {
                                viewModel.createNotebook(
                                    title = "Untitled notebook",
                                    colorArgb = notebookColorPalette[uiState.notebooks.size % notebookColorPalette.size],
                                    paperStyle = option.style
                                ).onSuccess { notebook ->
                                    onNavigateToCanvas(notebook.id, notebook.paperStyle)
                                }
                            }
                            // No-op until androidx.pdf lands (v1.1, D8/D12) — VAHI-502.
                            CreateNotebookChoice.ImportPdf -> Unit
                        }
                    }
                )
            }
        }

        // Layer 4: Snackbar host — surfaces "Undo" after a notebook is deleted.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = 16.dp)
                .zIndex(30f)
        )
    }
}

/* ---------- CREATE NOTEBOOK MODAL (WITH BLUR) ---------- */

/** What the create-notebook modal can return. Replaces the former `(String) -> Unit`
 *  callback that the caller matched against display labels — see `VAHI-211` and the
 *  "String-literal dispatch" row in docs/09-project-status.md's code-quality table. */
sealed interface CreateNotebookChoice {
    data class Paper(val style: PaperStyle) : CreateNotebookChoice
    data object ImportPdf : CreateNotebookChoice
}

@Composable
fun CreateNotebookOverlay(
    defaultPaperStyle: PaperStyle,
    onDismiss: () -> Unit,
    onOptionSelected: (CreateNotebookChoice) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.key == Key.Escape) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(680.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { },
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select the style that fits your needs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )

                Spacer(Modifier.height(24.dp))

                // 2x2 Grid of options. The user's remembered default (Settings →
                // Appearance → Default paper style) is marked, so the modal opens
                // already pointing at their usual choice.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreateNotebookOption(
                            title = "Blank",
                            subtitle = "Clean pages for free drawing",
                            glyph = "□",
                            isDefault = defaultPaperStyle == PaperStyle.BLANK,
                            onClick = { onOptionSelected(CreateNotebookChoice.Paper(PaperStyle.BLANK)) },
                            modifier = Modifier.weight(1f)
                        )
                        CreateNotebookOption(
                            title = "Ruled",
                            subtitle = "Lined pages for writing",
                            glyph = "≡",
                            isDefault = defaultPaperStyle == PaperStyle.RULED,
                            onClick = { onOptionSelected(CreateNotebookChoice.Paper(PaperStyle.RULED)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreateNotebookOption(
                            title = "Grid",
                            subtitle = "Grid pages for diagrams",
                            glyph = "⊞",
                            isDefault = defaultPaperStyle == PaperStyle.GRID,
                            onClick = { onOptionSelected(CreateNotebookChoice.Paper(PaperStyle.GRID)) },
                            modifier = Modifier.weight(1f)
                        )
                        CreateNotebookOption(
                            title = "Import PDF",
                            subtitle = "Annotate existing documents",
                            glyph = "↓",
                            isDefault = false,
                            onClick = { onOptionSelected(CreateNotebookChoice.ImportPdf) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateNotebookOption(
    title: String,
    subtitle: String,
    glyph: String,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        // The remembered default is outlined in the accent colour rather than given a
        // separate badge — one glance, no extra chrome.
        border = androidx.compose.foundation.BorderStroke(
            width = if (isDefault) 2.dp else 1.dp,
            color = if (isDefault) VahiAccentPurple else Color(0xFFE0E0E0)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                // Passed in rather than derived from `title` — the glyph used to be
                // chosen by matching the display string (VAHI-211).
                Text(
                    text = glyph,
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/* ---------- CARD CONTEXT MENU ---------- */

@Composable
fun CardContextMenu(
    isPinned: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit = {}
) {
    // "delete" and "pin"/"unpin" are real actions (VAHI-207). "edit"/"share"/"lock"
    // stay no-ops — rename needs a text-input dialog, share and lock need a backend
    // and a real credential story respectively. Neither is built yet; see
    // docs/09-project-status.md "Home screen features".
    val menuItems: List<Pair<String, () -> Unit>> = listOf(
        "delete" to onDelete,
        "edit" to {},
        "share" to {},
        (if (isPinned) "unpin" else "pin") to onTogglePin,
        "lock" to {}
    )

    Box(
        modifier = Modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .padding(12.dp)
    ) {
        Column {
            menuItems.forEach { (item, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            action()
                            onDismiss()
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8E8E8))
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = item,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

/* ---------- PROFILE OVERLAY (STEP 7) ---------- */

@Composable
fun ProfileOverlay(
    onDismiss: () -> Unit
) {
    val menuItems = listOf("Profile", "Account", "Notifications", "Upgrade to Plus", "Logout")

    Box(
        modifier = Modifier
            .width(240.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .padding(12.dp)
    ) {
        Column {
            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8E8E8))
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = item,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

/* ---------- MAIN CONTENT ---------- */

@Composable
private fun HomeContent(
    notebooks: List<Notebook>,
    showPinnedOnly: Boolean,
    onFilterChanged: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCreateNotebook: () -> Unit,
    onOpenNotebook: (Notebook) -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onCardMenuOpen: (CardMenuAnchor) -> Unit,
    onProfileButtonPositioned: (Offset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {

        TopBar(
            onSettings = onSettings,
            onProfile = onProfile,
            onProfileButtonPositioned = onProfileButtonPositioned
        )

        Spacer(Modifier.height(24.dp))

        FiltersRow(showPinnedOnly = showPinnedOnly, onFilterChanged = onFilterChanged)

        Spacer(Modifier.height(16.dp))

        ToolbarRow(
            onCreateNotebook = onCreateNotebook,
            searchQuery = searchQuery,
            onSearchQueryChanged = onSearchQueryChanged
        )

        Spacer(Modifier.height(24.dp))

        NotebookGrid(
            notebooks = notebooks,
            onCreateNotebook = onCreateNotebook,
            onOpenNotebook = onOpenNotebook,
            onCardMenuOpen = onCardMenuOpen
        )
    }
}

/* ---------- TOP BAR ---------- */

@Composable
private fun TopBar(
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onProfileButtonPositioned: (Offset) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vahi Logo
        Image(
            painter = painterResource(id = R.drawable.vahi_logo),
            contentDescription = "Vahi",
            modifier = Modifier.height(48.dp)
        )

        Spacer(Modifier.weight(1f))

        // Upgrade Button - Dark pill style
        Surface(
            onClick = {},
            shape = RoundedCornerShape(24.dp),
            color = VahiUpgradeButtonBg
        ) {
            Text(
                text = "Upgrade",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.width(12.dp))

        // Settings Button - Light pill with gear icon - navigates to SettingsScreen (VAHI-406)
        Surface(
            onClick = onSettings,
            shape = RoundedCornerShape(24.dp),
            color = VahiSettingsButtonBg
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF333333)
                )
                Text(
                    text = "Settings",
                    color = Color(0xFF333333),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Profile avatar — circular icon button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8D4A8)) // Warm beige background
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onProfileButtonPositioned(position)
                }
                .clickable { onProfile() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF5C4A2E)
            )
        }
    }
}

/* ---------- FILTERS ---------- */

@Composable
private fun FiltersRow(
    showPinnedOnly: Boolean,
    onFilterChanged: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" tab
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (!showPinnedOnly) VahiFilterSelected else Color.Transparent,
            modifier = Modifier.clickable { onFilterChanged(false) }
        ) {
            Text(
                text = "All",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }

        // "Pinned notes"
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (showPinnedOnly) VahiFilterSelected else Color.Transparent,
            modifier = Modifier.clickable { onFilterChanged(true) }
        ) {
            Text(
                text = "Pinned notes",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                color = if (showPinnedOnly) Color(0xFF333333) else Color(0xFF666666)
            )
        }

        // "Imported Books" — no-op; PDF import is v1.1 (D8/D12), no data to filter yet
        Text(
            text = "Imported Books",
            modifier = Modifier
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
    }
}

/* ---------- TOOLBAR ---------- */

@Composable
private fun ToolbarRow(
    onCreateNotebook: () -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search bar on the left — VAHI-408. Was a decorative Row with static
        // "Search notebooks..." text and no input; now a real field, filtering
        // visibleNotebooks by title (see the HomeScreen()-level searchQuery state).
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8F8F8),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8E8)),
            modifier = Modifier.width(280.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null, // decorative — the field's own label carries it
                    modifier = Modifier.size(16.dp),
                    tint = VahiTextPlaceholder
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Search notebooks" },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color(0xFF333333)),
                    cursorBrush = SolidColor(VahiAccentPurple),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search notebooks...",
                                    fontSize = 14.sp,
                                    color = VahiTextPlaceholder
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // View mode toggles
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grid view icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⊞",
                    fontSize = 16.sp,
                    color = Color(0xFF666666)
                )
            }

            // List view icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "≡",
                    fontSize = 18.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // "Most recent" — the repository already orders by updatedAt DESC, so this
        // label is honest even though the dropdown itself is still a no-op; a real
        // sort selector is future work (docs/12-ux-plan.md §4.3).
        Surface(
            onClick = {},
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Most recent",
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF333333)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // "Import" button - Blue filled pill
        Surface(
            onClick = {},
            shape = RoundedCornerShape(24.dp),
            color = VahiImportButtonBg
        ) {
            Text(
                text = "Import",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/* ---------- GRID ---------- */

@Composable
private fun NotebookGrid(
    notebooks: List<Notebook>,
    onCreateNotebook: () -> Unit,
    onOpenNotebook: (Notebook) -> Unit,
    onCardMenuOpen: (CardMenuAnchor) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        item {
            CreateNotebookCard(onCreateNotebook)
        }

        items(notebooks, key = { it.id }) { notebook ->
            NotebookCard(
                notebook = notebook,
                onOpen = { onOpenNotebook(notebook) },
                onMenuClick = { offset ->
                    onCardMenuOpen(CardMenuAnchor(notebook.id, offset))
                }
            )
        }
    }
}

/* ---------- CARDS ---------- */

@Composable
private fun CreateNotebookCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(260.dp, 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VahiCreateCardBg)
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Plus icon in a light purple circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VahiPlusCircleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF7B68EE) // Medium slate blue
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Create new notebook",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
private fun NotebookCard(
    notebook: Notebook,
    onOpen: () -> Unit,
    onMenuClick: (Offset) -> Unit
) {
    var menuButtonPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(260.dp, 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(notebook.colorArgb)) // ARGB int → Color at the UI boundary
            .clickable { onOpen() }
    ) {
        // Notebook icon in top-left — keyed off paper style, not a title-string
        // heuristic tied to sample data that no longer exists.
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (notebook.paperStyle) {
                    PaperStyle.BLANK -> "□"
                    PaperStyle.RULED -> "≡"
                    PaperStyle.GRID -> "⊞"
                },
                fontSize = 18.sp
            )
        }

        // Three-dot menu button in top-right. The touch target is a transparent 48dp
        // box; the visible chip stays 28dp. Previously the 28dp chip *was* the target
        // — well under the 48dp minimum, on the control that opens delete and pin.
        // Outer padding is 2dp rather than the old 12dp so the chip's visual centre
        // lands where it did before (12 + 28/2 == 2 + 48/2), and the dropdown anchor
        // stays on the chip itself so the menu still opens in the same place.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(48.dp)
                .clickable { onMenuClick(menuButtonPosition) }
                .semantics { contentDescription = "Notebook options" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.4f))
                    .onGloballyPositioned { coordinates ->
                        menuButtonPosition = coordinates.positionInRoot()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⋮⋮",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )
            }
        }

        // Title and last-edited time at the bottom. "Edited 2 hours ago" is more
        // useful than a creation date for navigating by (doc 15 §4.2 / doc 12 §4.1).
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = notebook.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Edited ${formatRelativeTime(notebook.updatedAt)}",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

/** A small, dependency-free relative-time formatter — "Edited 2 hours ago" rather
 *  than a raw timestamp. Not a general-purpose util yet; move it if a second
 *  caller shows up. */
private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 7 -> "$days day${if (days == 1L) "" else "s"} ago"
        else -> {
            val weeks = days / 7
            "$weeks week${if (weeks == 1L) "" else "s"} ago"
        }
    }
}
