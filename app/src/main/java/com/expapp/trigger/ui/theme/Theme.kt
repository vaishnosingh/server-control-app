package com.expapp.trigger.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.expapp.trigger.data.repository.ThemeMode
import kotlin.math.max
import kotlin.math.min

val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun resolveDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

/**
 * Generates a cohesive Material 3 / Google Pixel tonal ColorScheme from an arbitrary seed color.
 * Pure Kotlin math so it works identically at runtime and in JVM unit tests.
 */
fun generateSeedColorScheme(seedColor: Color, darkTheme: Boolean): ColorScheme {
    val (h, s, _) = rgbToHsl(seedColor.red, seedColor.green, seedColor.blue)
    val effectiveSat = if (s < 0.08f) 0.45f else s.coerceIn(0.35f, 0.85f)
    val tertiaryHue = (h + 52f) % 360f

    return if (darkTheme) {
        darkColorScheme(
            primary = hslColor(h, effectiveSat, 0.78f),
            onPrimary = hslColor(h, effectiveSat.coerceAtMost(0.75f), 0.16f),
            primaryContainer = hslColor(h, (effectiveSat * 0.85f).coerceIn(0.32f, 0.72f), 0.27f),
            onPrimaryContainer = hslColor(h, effectiveSat, 0.90f),
            secondary = hslColor(h, 0.25f, 0.75f),
            onSecondary = hslColor(h, 0.26f, 0.18f),
            secondaryContainer = hslColor(h, 0.22f, 0.25f),
            onSecondaryContainer = hslColor(h, 0.28f, 0.89f),
            tertiary = hslColor(tertiaryHue, 0.46f, 0.78f),
            onTertiary = hslColor(tertiaryHue, 0.48f, 0.17f),
            tertiaryContainer = hslColor(tertiaryHue, 0.36f, 0.26f),
            onTertiaryContainer = hslColor(tertiaryHue, 0.50f, 0.90f),
            background = hslColor(h, 0.08f, 0.075f),
            onBackground = hslColor(h, 0.08f, 0.91f),
            surface = hslColor(h, 0.08f, 0.075f),
            onSurface = hslColor(h, 0.08f, 0.91f),
            surfaceVariant = hslColor(h, 0.10f, 0.17f),
            onSurfaceVariant = hslColor(h, 0.10f, 0.76f),
            surfaceContainerLowest = hslColor(h, 0.08f, 0.055f),
            surfaceContainerLow = hslColor(h, 0.08f, 0.105f),
            surfaceContainer = hslColor(h, 0.08f, 0.13f),
            surfaceContainerHigh = hslColor(h, 0.09f, 0.16f),
            surfaceContainerHighest = hslColor(h, 0.10f, 0.20f),
            outline = hslColor(h, 0.10f, 0.56f),
            outlineVariant = hslColor(h, 0.10f, 0.26f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF5D1318),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = hslColor(h, effectiveSat, 0.39f),
            onPrimary = Color.White,
            primaryContainer = hslColor(h, effectiveSat.coerceIn(0.40f, 0.85f), 0.88f),
            onPrimaryContainer = hslColor(h, effectiveSat, 0.15f),
            secondary = hslColor(h, 0.24f, 0.41f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(h, 0.28f, 0.89f),
            onSecondaryContainer = hslColor(h, 0.32f, 0.16f),
            tertiary = hslColor(tertiaryHue, 0.44f, 0.41f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(tertiaryHue, 0.44f, 0.89f),
            onTertiaryContainer = hslColor(tertiaryHue, 0.48f, 0.16f),
            background = hslColor(h, 0.15f, 0.97f),
            onBackground = hslColor(h, 0.12f, 0.11f),
            surface = hslColor(h, 0.15f, 0.97f),
            onSurface = hslColor(h, 0.12f, 0.11f),
            surfaceVariant = hslColor(h, 0.16f, 0.89f),
            onSurfaceVariant = hslColor(h, 0.14f, 0.31f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(h, 0.15f, 0.95f),
            surfaceContainer = hslColor(h, 0.16f, 0.925f),
            surfaceContainerHigh = hslColor(h, 0.17f, 0.90f),
            surfaceContainerHighest = hslColor(h, 0.18f, 0.875f),
            outline = hslColor(h, 0.12f, 0.47f),
            outlineVariant = hslColor(h, 0.14f, 0.78f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

private fun hslColor(hue: Float, sat: Float, light: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = sat.coerceIn(0f, 1f)
    val l = light.coerceIn(0f, 1f)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val maxVal = max(r, max(g, b))
    val minVal = min(r, min(g, b))
    val d = maxVal - minVal
    val l = (maxVal + minVal) / 2f

    if (d == 0f) return Triple(210f, 0f, l)

    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    val h = when (maxVal) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    return Triple(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

@Composable
fun GlobalAppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> generateSeedColorScheme(Color(0xFF4C8DF6), darkTheme)
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

@Composable
fun ServerMaterialTheme(
    seedHex: String,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = resolveDarkTheme(themeMode)
    val seedColor = ServerAccents.parseColor(seedHex)
    val colorScheme = generateSeedColorScheme(seedColor, darkTheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

@Composable
fun ExpAppTheme(content: @Composable () -> Unit) {
    GlobalAppTheme(
        themeMode = ThemeMode.SYSTEM,
        dynamicColor = true,
        content = content
    )
}
