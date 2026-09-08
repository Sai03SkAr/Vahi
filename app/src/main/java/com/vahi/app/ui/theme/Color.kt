package com.vahi.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Vahi Color Palette
 * Centralized color definitions for consistent theming across the app.
 * No hardcoded colors should be used in screens - always reference these values.
 */

// Primary brand colors
val VahiPrimaryLight = Color(0xFF1A73E8)
val VahiPrimaryDark = Color(0xFF8AB4F8)

val VahiOnPrimaryLight = Color(0xFFFFFFFF)
val VahiOnPrimaryDark = Color(0xFF003A6E)

// Secondary colors
val VahiSecondaryLight = Color(0xFF5F6368)
val VahiSecondaryDark = Color(0xFFBDC1C6)

val VahiOnSecondaryLight = Color(0xFFFFFFFF)
val VahiOnSecondaryDark = Color(0xFF303030)

// Tertiary accent colors
val VahiTertiaryLight = Color(0xFF1E8E3E)
val VahiTertiaryDark = Color(0xFF81C995)

val VahiOnTertiaryLight = Color(0xFFFFFFFF)
val VahiOnTertiaryDark = Color(0xFF003A23)

// Background colors
val VahiBackgroundLight = Color(0xFFFFFBFE)
val VahiBackgroundDark = Color(0xFF121212)

val VahiOnBackgroundLight = Color(0xFF1C1B1F)
val VahiOnBackgroundDark = Color(0xFFE3E3E3)

// Surface colors
val VahiSurfaceLight = Color(0xFFFFFBFE)
val VahiSurfaceDark = Color(0xFF1E1E1E)

val VahiOnSurfaceLight = Color(0xFF1C1B1F)
val VahiOnSurfaceDark = Color(0xFFE3E3E3)

// Surface variants
val VahiSurfaceVariantLight = Color(0xFFE7E0EC)
val VahiSurfaceVariantDark = Color(0xFF2D2D2D)

val VahiOnSurfaceVariantLight = Color(0xFF49454F)
val VahiOnSurfaceVariantDark = Color(0xFFCAC4D0)

// Error colors
val VahiErrorLight = Color(0xFFD93025)
val VahiErrorDark = Color(0xFFF28B82)

val VahiOnErrorLight = Color(0xFFFFFFFF)
val VahiOnErrorDark = Color(0xFF601410)

// Outline colors
val VahiOutlineLight = Color(0xFF79747E)
val VahiOutlineDark = Color(0xFF938F99)

// HomeScreen specific colors
val VahiUpgradeButtonBg = Color(0xFF4A4A4A)         // Dark gray for upgrade button
val VahiSettingsButtonBg = Color(0xFFF5F5F5)        // Light gray for settings button
val VahiImportButtonBg = Color(0xFF1A73E8)          // Blue for import button
val VahiCardLavender = Color(0xFFC4BFFF)            // Lavender notebook card
val VahiFilterSelected = Color(0xFFF0F0F0)          // Selected filter pill background
val VahiCreateCardBg = Color(0xFFF5F5F5)            // Create notebook card background
val VahiPlusCircleBg = Color(0xFFE8E6FF)            // Light purple circle for plus icon

// AuthScreen & Input colors
val VahiInputBorder = Color(0xFFB8B5FF)             // Purple/lavender input border
val VahiInputBorderFocused = Color(0xFF8B85FF)      // Brighter purple when focused
val VahiAppleButtonBg = Color(0xFF000000)           // Black for Apple button

// Accent colors for vibrancy
val VahiAccentPurple = Color(0xFF7C4DFF)            // Vibrant purple accent

// Text colors that pass WCAG AA contrast on white (4.5:1 minimum).
// #999999/#AAAAAA (previously used for placeholders) fail AA — see docs/13, section 3.3.
val VahiTextPlaceholder = Color(0xFF767676)         // Placeholder text — lightest gray that still passes AA