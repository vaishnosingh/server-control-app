package com.expapp.trigger.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background System ─────────────────────────────────────────────────────
val BgDeep        = Color(0xFF060911)   // deepest black-navy
val BgBase        = Color(0xFF0C0F1A)   // main bg
val BgSurface     = Color(0xFF111827)   // card surface
val BgElevated    = Color(0xFF1A2235)   // elevated panels
val BgGlass       = Color(0x14FFFFFF)   // frosted glass overlay

// ── Border System ─────────────────────────────────────────────────────────
val BorderSubtle  = Color(0xFF1E2D47)   // very subtle border
val BorderDefault = Color(0xFF253450)   // standard border
val BorderStrong  = Color(0xFF334D73)   // stronger border on hover/focus

// ── Text ──────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF1F5F9)   // near-white
val TextSecondary = Color(0xFF8899B4)   // muted blue-grey
val TextMuted     = Color(0xFF4A5F7A)   // very muted

// ── Power On / Green ──────────────────────────────────────────────────────
val GreenVibrant  = Color(0xFF22D47E)   // primary green
val GreenDim      = Color(0xFF16A361)   // darker green
val GreenGlow     = Color(0x4022D47E)   // transparent glow
val GreenBg       = Color(0xFF071A11)   // dark green tinted bg
val GreenBorder   = Color(0xFF0D3D22)   // green border

// ── Reboot / Amber ────────────────────────────────────────────────────────
val AmberVibrant  = Color(0xFFFFB627)   // primary amber
val AmberDim      = Color(0xFFC47E00)   // darker amber
val AmberGlow     = Color(0x40FFB627)   // transparent glow
val AmberBg       = Color(0xFF191000)   // dark amber tinted bg
val AmberBorder   = Color(0xFF3D2C00)   // amber border

// ── Power Off / Red ───────────────────────────────────────────────────────
val RedVibrant    = Color(0xFFFF4757)   // primary red
val RedDim        = Color(0xFFCC2233)   // darker red
val RedGlow       = Color(0x40FF4757)   // transparent glow
val RedBg         = Color(0xFF190004)   // dark red tinted bg
val RedBorder     = Color(0xFF3D0010)   // red border

// ── Status Colors ─────────────────────────────────────────────────────────
val OnlineDot     = Color(0xFF22D47E)
val OnlineBg      = Color(0xFF071A11)
val OnlineBorder  = Color(0xFF0E3B1E)
val OnlineText    = Color(0xFF34EE91)

val OfflineDot    = Color(0xFFFF4757)
val OfflineBg     = Color(0xFF190004)
val OfflineBorder = Color(0xFF3D0010)
val OfflineText   = Color(0xFFFF6B78)

// ── Backwards-compat aliases (keep these so other files compile) ──────────
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
