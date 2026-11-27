package com.expapp.trigger.ui.theme

import androidx.compose.ui.graphics.Color

// ── Legacy / Fallback Dark Slate Tokens (preserved for non-M3 compatibility) ──
val BgDeep        = Color(0xFF070A10)
val BgBase        = Color(0xFF0A0E17)
val BgSurface     = Color(0xFF101623)
val BgCard        = Color(0xFF141C2B)
val BgElevated    = Color(0xFF1A2436)
val BgGlass       = Color(0x12FFFFFF)

val BorderSubtle  = Color(0xFF1F2B40)
val BorderDefault = Color(0xFF283752)
val BorderStrong  = Color(0xFF384C70)

val TextPrimary   = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF8899B4)
val TextMuted     = Color(0xFF4A5F7A)

// ── Semantic Status Palette (works across Light & Dark Pixel Material You) ──
val GreenVibrant  = Color(0xFF22D47E)
val GreenDim      = Color(0xFF16A361)
val GreenGlow     = Color(0x4022D47E)
val GreenBg       = Color(0xFF071A11)
val GreenBorder   = Color(0xFF0D3D22)

val AmberVibrant  = Color(0xFFFFB627)
val AmberDim      = Color(0xFFC47E00)
val AmberGlow     = Color(0x40FFB627)
val AmberBg       = Color(0xFF191000)
val AmberBorder   = Color(0xFF3D2C00)

val RedVibrant    = Color(0xFFFF4757)
val RedDim        = Color(0xFFCC2233)
val RedGlow       = Color(0x40FF4757)
val RedBg         = Color(0xFF190004)
val RedBorder     = Color(0xFF3D0010)

val OnlineDot     = Color(0xFF22D47E)
val OnlineBg      = Color(0xFF071A11)
val OnlineBorder  = Color(0xFF0E3B1E)
val OnlineText    = Color(0xFF34EE91)

val OfflineDot    = Color(0xFFFF4757)
val OfflineBg     = Color(0xFF190004)
val OfflineBorder = Color(0xFF3D0010)
val OfflineText   = Color(0xFFFF6B78)

// Adaptive semantic helper tokens for Material 3 Light & Dark surfaces
object PixelSemantic {
    fun onlineAccent(dark: Boolean) = if (dark) Color(0xFF5CE69C) else Color(0xFF0E8A4E)
    fun onlineContainer(dark: Boolean) = if (dark) Color(0xFF0F3D26) else Color(0xFFC9F7DE)
    fun onOnlineContainer(dark: Boolean) = if (dark) Color(0xFFA7F7CB) else Color(0xFF063920)

    fun warningAccent(dark: Boolean) = if (dark) Color(0xFFFFC857) else Color(0xFFB46900)
    fun warningContainer(dark: Boolean) = if (dark) Color(0xFF422D00) else Color(0xFFFFE5B4)
    fun onWarningContainer(dark: Boolean) = if (dark) Color(0xFFFFDF99) else Color(0xFF3B2100)

    fun infoAccent(dark: Boolean) = if (dark) Color(0xFF7CC8FF) else Color(0xFF0B69A3)
    fun infoContainer(dark: Boolean) = if (dark) Color(0xFF13364F) else Color(0xFFD2ECFF)
    fun onInfoContainer(dark: Boolean) = if (dark) Color(0xFFBBE3FF) else Color(0xFF082F4B)
}

// Backwards-compat aliases
val DarkBackground      = BgBase
val DarkSurface         = BgSurface
val DarkSurfaceVariant  = BgElevated
val DarkCardBorder      = BorderDefault
val PowerOnGreen        = GreenVibrant
val PowerOnGreenLight   = GreenVibrant
val PowerOnBorder       = GreenVibrant
val PowerOnGradient     = listOf(Color(0xFF0E3D22), Color(0xFF061610))
val RebootAmber         = AmberVibrant
val RebootAmberLight    = AmberVibrant
val RebootBorder        = AmberVibrant
val RebootGradient      = listOf(Color(0xFF3D2800), Color(0xFF1A1000))
val PowerOffRed         = RedVibrant
val PowerOffRedLight    = RedVibrant
val PowerOffBorder      = RedVibrant
val PowerOffGradient    = listOf(Color(0xFF3D0010), Color(0xFF190004))
val StatusOnlineBg      = OnlineBg
val StatusOnlineBorder  = OnlineBorder
val StatusOnlineText    = OnlineText
val StatusOfflineBg     = OfflineBg
val StatusOfflineBorder = OfflineBorder
val StatusOfflineText   = OfflineText
