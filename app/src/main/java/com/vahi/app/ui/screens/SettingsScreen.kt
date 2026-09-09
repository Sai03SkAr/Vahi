package com.vahi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vahi.app.BuildConfig
import com.vahi.app.R
import com.vahi.app.core.model.PaperStyle
import com.vahi.app.domain.model.AppTheme
import com.vahi.app.domain.model.Handedness
import com.vahi.app.domain.model.PaperTheme
import com.vahi.app.ui.feature.settings.SettingsViewModel
import com.vahi.app.ui.theme.VahiAccentPurple

private enum class SettingsSection { APPEARANCE, WRITING, STORAGE, ABOUT }

/**
 * `VAHI-406` — two-pane settings (list left, detail right), per
 * docs/15-ux-blueprint.md §6. The app-shell size gate (`AppRoot.kt`) already
 * guarantees Expanded before any route renders, so unlike the pre-navigation
 * `AuthScreen`/`SettingsOverlay` this never needs its own Compact branch.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var selectedSection by rememberSaveable { mutableStateOf(SettingsSection.APPEARANCE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            SettingsSectionList(
                selected = selectedSection,
                onSelect = { selectedSection = it },
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                when (selectedSection) {
                    SettingsSection.APPEARANCE -> AppearanceSection(
                        appTheme = uiState.preferences.appTheme,
                        paperTheme = uiState.preferences.paperTheme,
                        defaultPaperStyle = uiState.preferences.defaultPaperStyle,
                        onAppThemeChanged = viewModel::setAppTheme,
                        onPaperThemeChanged = viewModel::setPaperTheme,
                        onDefaultPaperStyleChanged = viewModel::setDefaultPaperStyle
                    )
                    SettingsSection.WRITING -> WritingSection(
                        handedness = uiState.preferences.handedness,
                        fingerDrawEnabled = uiState.preferences.fingerDrawEnabled,
                        onHandednessChanged = viewModel::setHandedness,
                        onFingerDrawChanged = viewModel::setFingerDrawEnabled
                    )
                    SettingsSection.STORAGE -> StorageSection(
                        notebookCount = uiState.notebookCount,
                        databaseSizeBytes = uiState.databaseSizeBytes
                    )
                    SettingsSection.ABOUT -> AboutSection()
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionList(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        SettingsSection.APPEARANCE to R.string.settings_section_appearance,
        SettingsSection.WRITING to R.string.settings_section_writing,
        SettingsSection.STORAGE to R.string.settings_section_storage,
        SettingsSection.ABOUT to R.string.settings_section_about
    )
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)) {
        items(items) { (section, labelRes) ->
            val isSelected = section == selected
            Surface(
                onClick = { onSelect(section) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) VahiAccentPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Box(modifier = Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = stringResource(labelRes),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = if (isSelected) VahiAccentPurple else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(16.dp))
}

/**
 * @param notYetApplied true for settings that persist correctly but that nothing in
 *   the app reads yet. Saying so on the control itself is the honest option: the
 *   alternative is a switch the user flips that silently changes nothing, which is
 *   worse than not offering it. Drop the flag as each one gets wired up.
 */
@Composable
private fun <T> SettingsChoiceRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    notYetApplied: Boolean = false,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (notYetApplied) {
            Text(
                text = stringResource(R.string.settings_not_applied_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, optionLabel) ->
                val isSelected = value == selected
                Surface(
                    onClick = { onSelect(value) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) VahiAccentPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = optionLabel,
                            color = if (isSelected) VahiAccentPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    appTheme: AppTheme,
    paperTheme: PaperTheme,
    defaultPaperStyle: PaperStyle,
    onAppThemeChanged: (AppTheme) -> Unit,
    onPaperThemeChanged: (PaperTheme) -> Unit,
    onDefaultPaperStyleChanged: (PaperStyle) -> Unit
) {
    Column {
        SettingSectionTitle(stringResource(R.string.settings_section_appearance))
        SettingsChoiceRow(
            label = stringResource(R.string.settings_app_theme),
            options = listOf(
                AppTheme.LIGHT to stringResource(R.string.settings_theme_light),
                AppTheme.DARK to stringResource(R.string.settings_theme_dark),
                AppTheme.SYSTEM to stringResource(R.string.settings_theme_system)
            ),
            selected = appTheme,
            onSelect = onAppThemeChanged
        )
        SettingsChoiceRow(
            label = stringResource(R.string.settings_paper_theme),
            options = listOf(
                PaperTheme.WHITE to stringResource(R.string.settings_paper_white),
                PaperTheme.CREAM to stringResource(R.string.settings_paper_cream),
                PaperTheme.DARK to stringResource(R.string.settings_paper_dark)
            ),
            selected = paperTheme,
            // Needs the display-time ink-colour mapping in the render path
            // (docs/22-decisions.md V1, docs/16-canvas-spec.md §7) — that is canvas
            // rendering work, gated behind the ink-engine decision (D6/VAHI-101).
            notYetApplied = true,
            onSelect = onPaperThemeChanged
        )
        SettingsChoiceRow(
            label = stringResource(R.string.settings_default_paper_style),
            options = listOf(
                PaperStyle.BLANK to stringResource(R.string.settings_paper_style_blank),
                PaperStyle.RULED to stringResource(R.string.settings_paper_style_ruled),
                PaperStyle.GRID to stringResource(R.string.settings_paper_style_grid)
            ),
            selected = defaultPaperStyle,
            onSelect = onDefaultPaperStyleChanged
        )
    }
}

@Composable
private fun WritingSection(
    handedness: Handedness,
    fingerDrawEnabled: Boolean,
    onHandednessChanged: (Handedness) -> Unit,
    onFingerDrawChanged: (Boolean) -> Unit
) {
    Column {
        SettingSectionTitle(stringResource(R.string.settings_section_writing))
        SettingsChoiceRow(
            label = stringResource(R.string.settings_handedness),
            options = listOf(
                Handedness.LEFT to stringResource(R.string.settings_handedness_left),
                Handedness.RIGHT to stringResource(R.string.settings_handedness_right)
            ),
            selected = handedness,
            // Tool-rail mirroring is VAHI-210, which depends on VAHI-203/112/401.
            notYetApplied = true,
            onSelect = onHandednessChanged
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_finger_draw),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.settings_finger_draw_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Filtering pointers by tool type is ink-engine work, and sits next to
                // bug #14 (a resting palm draws) which needs a real device to fix.
                Text(
                    text = stringResource(R.string.settings_not_applied_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = fingerDrawEnabled,
                onCheckedChange = onFingerDrawChanged,
                colors = SwitchDefaults.colors(checkedTrackColor = VahiAccentPurple)
            )
        }
    }
}

@Composable
private fun StorageSection(notebookCount: Int, databaseSizeBytes: Long) {
    Column {
        SettingSectionTitle(stringResource(R.string.settings_section_storage))

        val megabytes = databaseSizeBytes / (1024.0 * 1024.0)
        StorageRow(stringResource(R.string.settings_storage_used), stringResource(R.string.settings_storage_size_mb, megabytes))
        StorageRow(stringResource(R.string.settings_storage_notebook_count), notebookCount.toString())

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_storage_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AboutSection() {
    Column {
        SettingSectionTitle(stringResource(R.string.settings_section_about))
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${stringResource(R.string.settings_about_version)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
