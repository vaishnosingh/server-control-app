package com.expapp.trigger.ui.theme

import androidx.compose.ui.graphics.Color

data class ServerAccentPreset(
    val id: String,
    val label: String,
    val hex: String,
    val color: Color
)

object ServerAccents {
    val presets: List<ServerAccentPreset> = listOf(
        ServerAccentPreset("sage", "Pixel Sage", "#22D47E", Color(0xFF22D47E)),
        ServerAccentPreset("pixel_blue", "Pixel Blue", "#4C8DF6", Color(0xFF4C8DF6)),
        ServerAccentPreset("cyan", "Ocean Cyan", "#06B6D4", Color(0xFF06B6D4)),
        ServerAccentPreset("lavender", "Lavender", "#8B5CF6", Color(0xFF8B5CF6)),
        ServerAccentPreset("indigo", "Deep Indigo", "#6366F1", Color(0xFF6366F1)),
        ServerAccentPreset("rose", "Coral Rose", "#EC4899", Color(0xFFEC4899)),
        ServerAccentPreset("crimson", "Crimson", "#E14D58", Color(0xFFE14D58)),
        ServerAccentPreset("amber", "Warm Amber", "#E69D18", Color(0xFFE69D18)),
        ServerAccentPreset("terracotta", "Terracotta", "#E8713A", Color(0xFFE8713A)),
        ServerAccentPreset("teal", "Forest Teal", "#14B8A6", Color(0xFF14B8A6))
    )

    fun parseColor(hex: String?, fallback: Color = Color(0xFF22D47E)): Color {
        if (hex.isNullOrBlank()) return fallback
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> Color(0xFF000000L or clean.toLong(16))
                8 -> Color(clean.toLong(16))
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    fun nextSuggestedHex(existingCount: Int): String {
        return presets[existingCount % presets.size].hex
    }
}
