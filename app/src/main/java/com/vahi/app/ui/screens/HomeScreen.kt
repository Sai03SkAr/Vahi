package com.vahi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vahi.app.R
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
                // Card Context Menu - anchored to the three-dot button
                cardMenuAnchor?.let { anchor ->
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (anchor.offset.x - 140).roundToInt(), // Menu appears to the left of dots
                                y = (anchor.offset.y + 40).roundToInt()  // Below the dots
                            )
                        }
                    ) {
                        CardContextMenu(onDismiss = { closeDropdowns() })
                    }
                }

                // Settings Overlay - anchored below Settings button
                if (isSettingsOpen) {
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (settingsButtonPosition.x - 180).roundToInt(),
                                y = (settingsButtonPosition.y + 48).roundToInt()
                            )
                        }
                    ) {
                        SettingsOverlay(onDismiss = { closeDropdowns() })
                    }
                }

                // Profile Overlay - anchored below Profile avatar
                if (isProfileOpen) {
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (profileButtonPosition.x - 200).roundToInt(),
                                y = (profileButtonPosition.y + 48).roundToInt()
                            )
                        }
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

        Image(
            painter = painterResource(id = R.drawable.vahi_logo),
            contentDescription = "Vahi",
            modifier = Modifier.height(48.dp)
        )

        Spacer(Modifier.weight(1f))

        AssistChip(
            onClick = {},
            label = { Text("Upgrade") }
        )

        Spacer(Modifier.width(12.dp))

        AssistChip(
            onClick = onSettings,
            label = { Text("Settings") },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                onSettingsButtonPositioned(position)
            }
        )

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFD9D9D9))
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onProfileButtonPositioned(position)
                }
                .clickable { onProfile() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "P",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ---------- FILTERS ---------- */

@Composable
private fun FiltersRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AssistChip(onClick = {}, label = { Text("All") })
        Text("Pinned notes", modifier = Modifier.align(Alignment.CenterVertically))
        Text("Imported Books", modifier = Modifier.align(Alignment.CenterVertically))
    }
}

/* ---------- TOOLBAR ---------- */

@Composable
private fun ToolbarRow(onCreateNotebook: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Search", color = Color.Gray)

        Spacer(Modifier.weight(1f))

        Text("Most recent")

        Spacer(Modifier.width(16.dp))

        Button(onClick = {}) {
            Text("Import")
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
            .background(Color(0xFFF2F2F2))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF666666)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Create new notebook",
                fontSize = 18.sp,
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
            .padding(16.dp)
    ) {

        Column {
            Text(notebook.title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(notebook.created, fontSize = 14.sp)
        }

        // Three-dot menu button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .onGloballyPositioned { coordinates ->
                    menuButtonPosition = coordinates.positionInRoot()
                }
                .clickable {
                    onMenuClick(menuButtonPosition)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⋮",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
