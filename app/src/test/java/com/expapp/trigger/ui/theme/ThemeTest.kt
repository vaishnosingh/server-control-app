package com.expapp.trigger.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun generateSeedColorScheme_differsBetweenSeedsAndLightDark() {
        val blueDark = generateSeedColorScheme(Color(0xFF4C8DF6), darkTheme = true)
        val blueLight = generateSeedColorScheme(Color(0xFF4C8DF6), darkTheme = false)
        val amberDark = generateSeedColorScheme(Color(0xFFE69D18), darkTheme = true)

        assertNotEquals(blueDark.primary, amberDark.primary)
        assertNotEquals(blueDark.primaryContainer, amberDark.primaryContainer)
        assertNotEquals(blueDark.background, blueLight.background)
        // Dark background has low luminance, Light background has high luminance
        assertTrue(blueDark.background.red < 0.2f)
        assertTrue(blueLight.background.red > 0.85f)
    }
}
