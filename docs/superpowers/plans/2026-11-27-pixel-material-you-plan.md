# Google Pixel Material You UI Overhaul — Implementation Plan

> **Spec:** `docs/superpowers/specs/2026-11-27-pixel-material-you-design.md`

## Tasks
1. **Settings & Theme Foundations (`AppSettings.kt`, `res/font/*`, `Type.kt`, `Color.kt`, `Theme.kt`, `ServerAccents.kt`, `themes.xml`)**
   - Add `ThemeMode` (`SYSTEM`, `LIGHT`, `DARK`) and `isDynamicColorEnabled` (`true` by default) to `AppSettings`, plus unit tests in `AppSettingsTest.kt`.
   - Bundle Quicksand, Open Sans, and Fira Code fonts in `res/font/` and create `Type.kt` with complete Material 3 `Typography` (`AppTypography`) + `MonoFontFamily` helper for tabular telemetry.
   - Build a pure-Kotlin Material 3 Seed Tonal Palette Engine in `Theme.kt` (`seedColorScheme(seedColor, darkTheme)` + `ServerMaterialTheme(seedHex, themeMode)` + `GlobalAppTheme(themeMode, dynamicColor)`), with unit tests verifying Light/Dark contrast and tonal outputs.
2. **Core Interactive Components Overhaul (`HugeActionButton.kt`, `StatusBanner.kt`, `AccentColorPicker.kt`)**
   - Restyle power action cards into Google Pixel Material You tonal pill containers (`24dp` corners, `primaryContainer` / `secondaryContainer` / `errorContainer` tonal fills, clean `48dp` icon badges, zero heavy neon borders/shadows).
   - Restyle `StatusBanner` and `AccentColorDialog` / `AccentColorPickerRow` with Material 3 tonal surfaces (`28dp` corners) and dynamic theme colors.
3. **Dashboard & Service Detail Overhaul (`DashboardComponents.kt`, `DashboardScreen.kt`)**
   - Tonal `24dp`–`28dp` CPU & Memory hero cards with rounded gauges (`tnum` typography) and thick `8dp` Storage progress bars.
   - Android Settings-style grouped `ServiceList` container (`24dp` corners, `64dp` rows, status pill + boot pill + `ChevronRight`, zero inline action buttons).
   - Overhaul `ServiceDetailView` with Pixel back header, hero tonal card, M3 action tiles (`Start`/`Stop`/`Restart`), Pixel M3 `Switch` with thumb icon + Enable/Disable buttons, and `FilterChip` delay scheduler.
4. **Main Shell, Control, Fleet & Onboarding (`MainScreen.kt`, `DevicePage.kt`, `FleetOverviewPage.kt`, `AddDevicePage.kt`)**
   - 3-destination Pixel Bottom `NavigationBar` (`Control`, `SysPulse`, `Fleet`) with top app bar actions (Update indicator, History, Settings) and synchronized `selectedServerId`.
   - `Control` retains `HorizontalPager` over `devices` + top server switcher chip bar + empty state CTA.
   - `Fleet` displays all servers wrapped in their own `ServerMaterialTheme(dev.accentColorHex)` tonal cards + `ExtendedFloatingActionButton` opening `AddDevicePage` as a full-screen route.
   - Dynamic edge-to-edge system bar styling in `MainActivity.kt`.
5. **Dialogs Overhaul (`SettingsDialog.kt`, `HistoryDialog.kt`, `UpdateDialog.kt`, `NfcWriteDialog.kt`, `ActionConfirmDialog.kt`)**
   - Update all dialogs to `28dp` M3 tonal `Surface`, Pixel Settings Appearance selector (`System / Light / Dark` + Dynamic Color toggle), readable `Typography`, and M3 buttons.
6. **Build, Unit Tests, Lint & Release (`v1.5.0`)**
   - Verify with `./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug` and push to both GitHub and Forge.
