# Google Pixel Material You UI Overhaul — Design Specification

## Intent & Why
Replace the current dark-navy, 1dp-bordered card aesthetic and ad-hoc text sizes (`9sp–12sp` inline monospace) with a cohesive **Google Pixel Material You (Material 3)** design system across every screen, dialog, component, and theme token. Every surface uses tonal elevation, generous pill/rounded geometry, bundled geometric & readable fonts with a unified `Typography` scale, adaptive Light & Dark themes (plus Dynamic Color on Android 12+), and per-server tonal seed palettes so multi-server navigation feels genuinely native to a Pixel device.

---

## Goals & Non-Goals

### Goals
1. **Material 3 Tonal Color System (`Theme.kt`, `Color.kt`, `ServerAccents.kt`)**:
   - Adaptive Light and Dark themes (`System Default` / `Light` / `Dark` preference persisted in `AppSettings`, plus optional Android 12+ `dynamicDarkColorScheme` / `dynamicLightColorScheme` toggle for global chrome).
   - Pure-Kotlin Material 3 **Per-Server Seed Tonal Generator**: derives a complete harmonious `ColorScheme` (`primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `secondary`, `secondaryContainer`, `tertiary`, `surface`, `surfaceContainerLowest`..`Highest`, `outlineVariant`) from each server's assigned `accentColorHex` so each server page wraps itself in authentic Pixel tonal surfaces rather than harsh colored borders.
2. **Bundled Custom Pixel-Style Font Family & Typography Scale (`res/font/` + `Type.kt`)**:
   - Bundle open-source fonts right in `app/src/main/res/font/`:
     - **Display / Headings**: **Quicksand** (`Medium`, `SemiBold` via `Medium`/`Bold`) — rounded geometric letterforms matching Google Sans / Pixel Clock geometry.
     - **Body / UI Labels**: **Open Sans** (`Regular`, `Medium`/`SemiBold`, `Bold`) — crystal-clear Android legibility from `12sp` to `16sp`.
     - **Tabular Telemetry / Technical Units**: **Fira Code** (`Regular`, `Medium`, `Bold`) — exclusively for hostnames, systemd units (`xrdp.service`), PIDs, IPs, and numeric metrics, using `fontFeatureSettings = "tnum"`.
   - Eliminate all tiny `9sp`/`10sp` micro-labels; establish a single Material 3 `Typography` scale (`headlineMedium` 26sp, `titleLarge` 21sp, `titleMedium` 16sp, `bodyLarge` 15sp, `bodyMedium` 14sp, `labelLarge` 14sp, `labelMedium` 12sp).
3. **Pixel 3-Destination Layout Architecture (`MainScreen.kt`)**:
   - **Bottom `NavigationBar` with Material 3 Pill Indicator**:
     - **Control**: Active server workspace — Server switcher pill bar (`● ServerName`) at top, spacious Hero Identity card (`28dp` corner radius, tonal `surfaceContainerHigh`), compact 4-stat Telemetry Glance pill card, and large Material You **Tonal Action Cards** (`Power On`, `Reboot`, `Power Off`, `24dp` rounded containers using tonal semantic containers rather than dark-bordered slabs).
     - **SysPulse**: Hardware Telemetry (`28dp` tonal CPU & Memory cards with rounded gauges + thick `8dp` Storage bars) + **Pixel Android Settings-style Grouped Service List** (`24dp` rounded surface container, comfortable `64dp` rows, leading status icon container, `16sp` name + `13sp` unit + `RUNNING`/`STOPPED` & `Boot On/Off` chips, trailing `ChevronRight` — zero inline action buttons). Tapping any service row pushes **`ServiceDetailView`** cleanly (`BackHandler` supported) with Start/Stop/Restart pills, Pixel `Switch` (with check/close icon thumb) + Enable/Disable on Boot buttons, and scheduled delay builder.
     - **Fleet**: Multi-server console where each server card (`28dp` rounded) renders in its **own Material You Tonal Seed Theme** with online/offline status pill, IP & uptime, 1-tap Accent Theme picker button, quick Control / SysPulse pills, KPI summary bar, and Material 3 `ExtendedFloatingActionButton` (`+ Add Server`).
   - Clean `TopAppBar` / `CenterAlignedTopAppBar` housing Update pill (when ready), History, and Settings (`48dp` M3 touch targets).
4. **Full Overhaul of All Screens & Dialogs**:
   - Refactor `AddDevicePage`, `Edit Server` modal, `SettingsDialog`, `HistoryDialog`, `UpdateDialog`, `NfcWriteDialog`, `ActionConfirmDialog`, `AccentColorDialog`, and `StatusBanner` onto Material 3 tonal `Surface` (`28dp` corners), M3 `Switch`, M3 `FilterChip`, `FilledTonalButton`, and `Typography` tokens.
   - Update `MainActivity` edge-to-edge system bars (`enableEdgeToEdge(SystemBarStyle.auto(...))`) so status and navigation bars match Light/Dark dynamically.

### Non-Goals & Preservation Guarantees
- **No regressions to backend/worker contracts**: R2-first endpoint resolution + fallback (`R2ConfigParser`, `FallbackConfig`), in-place server list preservation (`DeviceRepository`), SysPulse `/api/service/enable`, `/api/service/disable`, 404 fallback, biometric gates, background WorkManager polling, widget behavior, and monochrome/smartwatch notification icons stay unchanged and covered by unit tests.
- Home-screen widget XML layout (`widget_server_control.xml`) stays functional (launcher `RemoteViews` cannot render Compose MaterialTheme dynamically).

---

## Approaches Considered

### Approach A: Dark-Only Tonal Restyle
- Keeps the app permanently dark, updates shapes to `24dp`–`28dp` Material 3 tonal cards and adds Roboto/Fira fonts.
- *Trade-off*: Simpler theme logic, misses true Google Pixel Material You identity (Pixel apps adapt smoothly to Light mode, Dark mode, and Wallpaper Dynamic Color).

### Approach B (Selected): Full Adaptive Pixel Material You Design System
- Implements dynamic Light + Dark `ColorScheme`s, System/Light/Dark setting, Android 12+ Dynamic Color option, and an algorithmic per-server **Seed Tonal Palette Generator** so every server page feels genuinely Material You in both Light and Dark mode, paired with bundled Quicksand / Open Sans / Fira Code typography and a 3-destination Pixel Bottom `NavigationBar` (`Control`, `SysPulse`, `Fleet`).
- *Trade-off*: Touches every Compose UI file + `AppSettings` + `MainActivity` edge-to-edge bar styling; cleanly verifiable via unit tests on `AppSettings` + tonal color generator + `./gradlew testDebugUnitTest assembleDebug assembleRelease lintDebug`.

---

## Builder Calls & Confirmed Architectural Decisions
- **Theme Boundary & Defaults (Confirmed)**:
  - `AppSettings` defaults to `ThemeMode.SYSTEM` (`System Default` / `Light` / `Dark` selector in Settings) with `isDynamicColorEnabled = true` on Android 12+.
  - App-wide screens (`Fleet`, `Add Server`, `Settings`, `History`, `Update`) follow the System/User theme mode + Android Dynamic Color.
  - Per-server workspaces (`Control`, `SysPulse`, `ServiceDetailView`, and each card on `Fleet`) always wrap in that server's **Material You Seed Tonal `ColorScheme`** (derived for both Light & Dark modes from `device.accentColorHex`) so server identity colors remain unmistakable.
- **Navigation, HorizontalPager & Add Server Flow (Confirmed)**:
  - **`Control` tab** retains **`HorizontalPager`** strictly across configured `devices` (keyed by `device.id`), bidirectionally synchronized with `selectedServerId` and the top server pill strip.
  - **`SysPulse` tab** displays the currently selected server (`selectedServerId`, synced with `Control` + top switcher pill) and launches full-screen `ServiceDetailView` when tapping a service row (`BackHandler` supported).
  - **`AddDevicePage`** moves out of the swipe pager into a dedicated full-screen route (hiding `NavigationBar`, `BackHandler` supported) opened from **`Fleet`'s `+ Add Server` `ExtendedFloatingActionButton`** and from empty-state CTAs on `Control` / `SysPulse` when `devices.isEmpty()`.
- **Font Bundling**: Copy `Quicksand-Medium.ttf` & `Quicksand-Bold.ttf`, `OpenSans-Regular.ttf` / `Semibold` / `Bold`, and `FiraCode-Regular.ttf` / `Medium` / `Bold` from `/usr/share/fonts/truetype/` directly into `app/src/main/res/font/` (zero runtime Google Play Services GMS font fetch needed, 100% deterministic offline & CI builds).
- **Material 3 BOM**: Check and align M3 APIs compatible with existing Kotlin `1.9.22` + `kotlinCompilerExtensionVersion = "1.5.8"` so CI (`release.yml`) builds cleanly without Kotlin version bumps.
