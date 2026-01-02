package com.vahi.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Vahi Dark Color Scheme
 */
private val VahiDarkColorScheme = darkColorScheme(
    primary = VahiPrimaryDark,
    onPrimary = VahiOnPrimaryDark,
    secondary = VahiSecondaryDark,
    onSecondary = VahiOnSecondaryDark,
    tertiary = VahiTertiaryDark,
    onTertiary = VahiOnTertiaryDark,
    background = VahiBackgroundDark,
    onBackground = VahiOnBackgroundDark,
    surface = VahiSurfaceDark,
    onSurface = VahiOnSurfaceDark,
    surfaceVariant = VahiSurfaceVariantDark,
    onSurfaceVariant = VahiOnSurfaceVariantDark,
    error = VahiErrorDark,
    onError = VahiOnErrorDark,
    outline = VahiOutlineDark
)

/**
 * Vahi Light Color Scheme
 */
private val VahiLightColorScheme = lightColorScheme(
    primary = VahiPrimaryLight,
    onPrimary = VahiOnPrimaryLight,
    secondary = VahiSecondaryLight,
    onSecondary = VahiOnSecondaryLight,
    tertiary = VahiTertiaryLight,
    onTertiary = VahiOnTertiaryLight,
    background = VahiBackgroundLight,
    onBackground = VahiOnBackgroundLight,
    surface = VahiSurfaceLight,
    onSurface = VahiOnSurfaceLight,
    surfaceVariant = VahiSurfaceVariantLight,
    onSurfaceVariant = VahiOnSurfaceVariantLight,
    error = VahiErrorLight,
    onError = VahiOnErrorLight,
    outline = VahiOutlineLight
)

/**
 * Vahi App Theme
 * Provides Material 3 theming with centralized colors, typography, and shapes.
 *
 * @param darkTheme Whether to use dark theme colors
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content The composable content to be themed
 */
@Composable
fun VahiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VahiDarkColorScheme
        else -> VahiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VahiTypography,
        shapes = VahiShapes,
        content = content
    )
}