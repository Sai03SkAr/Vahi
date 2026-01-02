package com.vahi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Vahi Shapes
 * Centralized shape definitions for consistent corner radii across the app.
 */
val VahiShapes = Shapes(
    // Extra small - for chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for text fields, small cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for modals, bottom sheets
    large = RoundedCornerShape(16.dp),
    
    // Extra large - for expanded containers
    extraLarge = RoundedCornerShape(24.dp)
)
