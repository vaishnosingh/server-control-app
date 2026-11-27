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
        ServerAccentPreset("emerald", "Emerald", "#22D47E", Color(0xFF22D47E)),
        ServerAccentPreset("cyan", "Cyan", "#06B6D4", Color(0xFF06B6D4)),
        ServerAccentPreset("sky", "Sky", "#38BDF8", Color(0xFF38BDF8)),
        ServerAccentPreset("cobalt", "Cobalt", "#3B82F6", Color(0xFF3B82F6)),
        ServerAccentPreset("indigo", "Indigo", "#6366F1", Color(0xFF6366F1)),
        ServerAccentPreset("violet", "Violet", "#8B5CF6", Color(0xFF8B5CF6)),
        ServerAccentPreset("magenta", "Magenta", "#EC4899", Color(0xFFEC4899)),
        ServerAccentPreset("rose", "Rose", "#F43F5E", Color(0xFFF43F5E)),
        ServerAccentPreset("amber", "Amber", "#FFB627", Color(0xFFFFB627)),
        ServerAccentPreset("orange", "Orange", "#F97316", Color(0xFFF97316)),
        ServerAccentPreset("teal", "Teal", "#14B8A6", Color(0xFF14B8A6))
    )

    fun parseColor(hex: String?, fallback: Color = GreenVibrant): Color {
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
