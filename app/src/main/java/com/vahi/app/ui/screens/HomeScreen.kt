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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vahi.app.R
import com.vahi.app.ui.theme.*
import kotlin.math.roundToInt

/* ---------- DATA ---------- */

data class Notebook(
    val id: String,
    val title: String,
    val created: String,
    val color: Color
)

data class CardMenuAnchor(
    val notebookId: String,
    val offset: Offset
)

/* ---------- SCREEN ---------- */

@Composable
fun HomeScreen(
    onNavigateToCanvas: (NotebookType) -> Unit = {}
) {
    // Density for converting captured pointer/layout offsets (px) to dp for overlay positioning —
    // see docs/09, bug #3: these offsets come from positionInRoot() and are in pixels, not dp.
    val density = LocalDensity.current

    // State for full-screen modal
    var isCreateNotebookOpen by remember { mutableStateOf(false) }
    
    // State for dropdown overlays
    var cardMenuAnchor by remember { mutableStateOf<CardMenuAnchor?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isProfileOpen by remember { mutableStateOf(false) }
    
    // Button positions for anchoring overlays
    var settingsButtonPosition by remember { mutableStateOf(Offset.Zero) }
    var profileButtonPosition by remember { mutableStateOf(Offset.Zero) }

    // Helper functions for overlay management
    fun openCardMenu(anchor: CardMenuAnchor) {
        isSettingsOpen = false
        isProfileOpen = false
        cardMenuAnchor = anchor
    }

    fun openSettings() {
        cardMenuAnchor = null
        isProfileOpen = false
        isSettingsOpen = true
    }

    fun openProfile() {
        cardMenuAnchor = null
        isSettingsOpen = false
        isProfileOpen = true
    }

    fun closeDropdowns() {
        cardMenuAnchor = null
        isSettingsOpen = false
        isProfileOpen = false
    }

    fun openCreateNotebook() {
        closeDropdowns()
        isCreateNotebookOpen = true
    }

    // Determine if any dropdown is open
    val isAnyDropdownOpen = cardMenuAnchor != null || isSettingsOpen || isProfileOpen

    // Handle back button for all overlays
    BackHandler(enabled = isCreateNotebookOpen || isAnyDropdownOpen) {
        if (isCreateNotebookOpen) {
            isCreateNotebookOpen = false
        } else {
            closeDropdowns()
        }
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
                onCreateNotebook = { openCreateNotebook() },
                onSettings = { openSettings() },
                onProfile = { openProfile() },
                onCardMenuOpen = { anchor -> openCardMenu(anchor) },
                onSettingsButtonPositioned = { position -> settingsButtonPosition = position },
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
                    val topPadding = with(density) { anchor.offset.y.toDp() } + 40.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding, end = 24.dp)
                    ) {
                        CardContextMenu(onDismiss = { closeDropdowns() })
                    }
                }

                // Settings Overlay - anchored below Settings button, positioned within screen
                if (isSettingsOpen) {
                    val topPadding = with(density) { settingsButtonPosition.y.toDp() } + 48.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding, end = 60.dp)
                    ) {
                        SettingsOverlay(onDismiss = { closeDropdowns() })
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
                    onDismiss = { isCreateNotebookOpen = false },
                    onOptionSelected = { option ->
                        isCreateNotebookOpen = false
                        when (option) {
                            "Blank" -> onNavigateToCanvas(NotebookType.BLANK)
                            "Ruled" -> onNavigateToCanvas(NotebookType.RULED)
                            "Grid" -> onNavigateToCanvas(NotebookType.GRID)
                            "Import PDF" -> { /* Import handled separately */ }
                        }
                    }
                )
            }
        }
    }
}

/* ---------- CREATE NOTEBOOK MODAL (WITH BLUR) ---------- */

@Composable
fun CreateNotebookOverlay(
    onDismiss: () -> Unit,
    onOptionSelected: (String) -> Unit
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

                // 2x2 Grid of options
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreateNotebookOption(
                            title = "Blank",
                            subtitle = "Clean pages for free drawing",
                            onClick = { onOptionSelected("Blank") },
                            modifier = Modifier.weight(1f)
                        )
                        CreateNotebookOption(
                            title = "Ruled",
                            subtitle = "Lined pages for writing",
                            onClick = { onOptionSelected("Ruled") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CreateNotebookOption(
                            title = "Grid",
                            subtitle = "Grid pages for diagrams",
                            onClick = { onOptionSelected("Grid") },
                            modifier = Modifier.weight(1f)
                        )
                        CreateNotebookOption(
                            title = "Import PDF",
                            subtitle = "Annotate existing documents",
                            onClick = { onOptionSelected("Import PDF") },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
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
                Text(
                    text = when (title) {
                        "Blank" -> "□"
                        "Ruled" -> "≡"
                        "Grid" -> "⊞"
                        "Import PDF" -> "↓"
                        else -> "•"
                    },
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
    onDismiss: () -> Unit
) {
    val menuItems = listOf("delete", "edit", "share", "pin", "lock")

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

/* ---------- SETTINGS OVERLAY ---------- */

@Composable
fun SettingsOverlay(
    onDismiss: () -> Unit
) {
    val menuItems = listOf("Profile", "Preferences", "Help", "Logout")

    Box(
        modifier = Modifier
            .width(260.dp)
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
    onCreateNotebook: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onCardMenuOpen: (CardMenuAnchor) -> Unit,
    onSettingsButtonPositioned: (Offset) -> Unit,
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
            onSettingsButtonPositioned = onSettingsButtonPositioned,
            onProfileButtonPositioned = onProfileButtonPositioned
        )

        Spacer(Modifier.height(24.dp))

        FiltersRow()

        Spacer(Modifier.height(16.dp))

        ToolbarRow(onCreateNotebook)

        Spacer(Modifier.height(24.dp))

        NotebookGrid(
            onCreateNotebook = onCreateNotebook,
            onCardMenuOpen = onCardMenuOpen
        )
    }
}

/* ---------- TOP BAR ---------- */

@Composable
private fun TopBar(
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onSettingsButtonPositioned: (Offset) -> Unit,
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

        // Settings Button - Light pill with gear icon
        Surface(
            onClick = onSettings,
            shape = RoundedCornerShape(24.dp),
            color = VahiSettingsButtonBg,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onSettingsButtonPositioned(position)
            }
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

        // Profile Avatar - Circular with colored background
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8D4A8)) // Warm beige background
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onProfileButtonPositioned(position)
                }
                .clickable { onProfile() },
            contentAlignment = Alignment.Center
        ) {
            // Placeholder bird emoji or icon
            Text(
                text = "🐦",
                fontSize = 20.sp
            )
        }
    }
}

/* ---------- FILTERS ---------- */

@Composable
private fun FiltersRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" tab - selected state with pill background
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VahiFilterSelected,
            modifier = Modifier.clickable { }
        ) {
            Text(
                text = "All",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }

        // "Pinned notes" - plain text
        Text(
            text = "Pinned notes",
            modifier = Modifier
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        // "Imported Books" - plain text
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
private fun ToolbarRow(onCreateNotebook: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search bar on the left
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
                // Search icon
                Text(
                    text = "🔍",
                    fontSize = 14.sp
                )
                Text(
                    text = "Search notebooks...",
                    fontSize = 14.sp,
                    color = VahiTextPlaceholder
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
            // Checkmark - selected view indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF333333)
                )
            }

            // Grid view icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                // 4-square grid icon using text
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
                // List lines icon using text
                Text(
                    text = "≡",
                    fontSize = 18.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // "Most recent" dropdown
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
    onCreateNotebook: () -> Unit,
    onCardMenuOpen: (CardMenuAnchor) -> Unit
) {
    val notebooks = listOf(
        Notebook("1", "Maths notes", "Created 4 jan 2025", Color(0xFFBABEFF)),
        Notebook("2", "Python Problems", "Created 23 dec 2025", Color(0xFFD3C3C3))
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        item {
            CreateNotebookCard(onCreateNotebook)
        }

        items(notebooks) { notebook ->
            NotebookCard(
                notebook = notebook,
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
    onMenuClick: (Offset) -> Unit
) {
    var menuButtonPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(260.dp, 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(notebook.color)
    ) {
        // Notebook icon in top-left
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            // Icon placeholder - abacus for Maths, laptop for Python, etc.
            Text(
                text = if (notebook.title.contains("Maths", ignoreCase = true)) "🧮" 
                       else if (notebook.title.contains("Python", ignoreCase = true)) "💻"
                       else "📓",
                fontSize = 20.sp
            )
        }

        // Three-dot menu button in top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.4f))
                .onGloballyPositioned { coordinates ->
                    menuButtonPosition = coordinates.positionInRoot()
                }
                .clickable {
                    onMenuClick(menuButtonPosition)
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

        // Title and date at the bottom
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
                text = notebook.created,
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}
