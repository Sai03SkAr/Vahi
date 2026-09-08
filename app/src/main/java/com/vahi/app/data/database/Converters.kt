package com.vahi.app.data.database

import androidx.room.TypeConverter
import com.vahi.app.core.model.PaperStyle

/**
 * Persist enums by name, never by ordinal — Room's default. An ordinal breaks
 * silently the day someone inserts a value in the middle of the enum and every
 * existing row's meaning shifts (docs/17-data-spec.md).
 */
class Converters {
    @TypeConverter
    fun paperStyleToString(value: PaperStyle): String = value.name

    @TypeConverter
    fun stringToPaperStyle(value: String): PaperStyle = PaperStyle.valueOf(value)
}
