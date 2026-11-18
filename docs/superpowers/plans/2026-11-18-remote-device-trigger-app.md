# Remote Device Trigger Android Application Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android application in `Desktop/Vaishno_workspace/experiments/exp-app` featuring a horizontal swipe interface with 3 huge buttons (Power On, Power Off, Reboot) per device, dynamic Cloudflare R2 JSON configuration, secret key authentication, and persistent local storage.

**Architecture:** Single-activity Kotlin application with Jetpack Compose Material 3 UI. It uses a `HorizontalPager` for multi-device management, `OkHttp` for asynchronous HTTP POST dispatches with `X-Secret-Key` header and JSON body, `Gson` for R2 endpoint parsing, and `SharedPreferences` for local device persistence.

**Tech Stack:** Kotlin 1.9+, Android SDK 34 (minSdk 24), Jetpack Compose (Material 3, Foundation Pager), OkHttp 4.12, Gson 2.10, JUnit 4.

**Spec:** [docs/superpowers/specs/2026-11-18-remote-device-trigger-app-design.md](file:///home/creserver/Desktop/Vaishno_workspace/experiments/exp-app/docs/superpowers/specs/2026-11-18-remote-device-trigger-app-design.md)

## Global Constraints
- Target directory is strictly `/home/creserver/Desktop/Vaishno_workspace/experiments/exp-app`.
- Android minSdk 24, targetSdk 34.
- Networking uses OkHttp; requests send `X-Secret-Key: <secretKey>` header and `{"node": "<node>"}` JSON body on POST calls.
- Flexible JSON parser supporting case variants (`poweron`, `powerOn`, `power_on`, etc.).
- Complete standalone project structure including Gradle wrapper files so it can be built and opened in Android Studio or built via CLI.

## Review Focus
1. R2 JSON response missing one or more required keys (`poweron`, `poweroff`, `reboot`): must display clear validation error and prevent saving.
2. Network timeout or unreachable host: must update UI state with failure message and not freeze the app.
3. Empty device list on initial launch: must guide user to the Add Device tab without crashes.
4. Special characters in `secretKey` or `node`: must be properly escaped in JSON payload and preserved in HTTP headers.
5. In-flight request cancellation or rapid double-tap: buttons must disable while request is pending to prevent duplicate triggers.

---

### Task 1: Android Gradle Scaffolding & Manifest Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`

**Interfaces:**
- Produces: Complete Android Gradle build system and app module configuration with Jetpack Compose, OkHttp, and Gson dependencies.

- [ ] **Step 1: Create root settings and build Gradle scripts**
Write `settings.gradle.kts` with root project name `"exp-app"` and include `":app"`. Configure plugin repositories and dependency repositories (`google()`, `mavenCentral()`).

- [ ] **Step 2: Create root build.gradle.kts and gradle.properties**
Write `build.gradle.kts` registering Android and Kotlin plugins. In `gradle.properties`, set `android.useAndroidX=true` and `org.gradle.jvmargs=-Xmx2048m`.

- [ ] **Step 3: Create app/build.gradle.kts**
Configure namespace `com.expapp.trigger`, compileSdk 34, minSdk 24, targetSdk 34, Compose compiler, and dependencies:
- `androidx.compose.ui:ui`
- `androidx.compose.material3:material3`
- `androidx.compose.foundation:foundation`
- `androidx.activity:activity-compose`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `com.google.code.gson:gson:2.10.1`
- `junit:junit:4.13.2`

- [ ] **Step 4: Create app/src/main/AndroidManifest.xml**
Include `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`. Declare `MainActivity` with `android.intent.action.MAIN` and `android.intent.category.LAUNCHER`.

- [ ] **Step 5: Setup Gradle wrapper scripts**
Create `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` (specifying Gradle 8.4+). Make `gradlew` executable.

- [ ] **Step 6: Verify file structure**
Run `ls -la app/` and verify manifest and build scripts exist.

---

### Task 2: Data Models, R2 JSON Parser, and Device Repository

**Files:**
- Create: `app/src/main/java/com/expapp/trigger/data/model/DeviceConfig.kt`
- Create: `app/src/main/java/com/expapp/trigger/data/parser/R2ConfigParser.kt`
- Create: `app/src/main/java/com/expapp/trigger/data/repository/DeviceRepository.kt`
- Test: `app/src/test/java/com/expapp/trigger/data/parser/R2ConfigParserTest.kt`
- Test: `app/src/test/java/com/expapp/trigger/data/repository/DeviceRepositoryTest.kt`

**Interfaces:**
- Produces:
  - `DeviceConfig(id: String, name: String, node: String, secretKey: String, r2Url: String, powerOnUrl: String, powerOffUrl: String, rebootUrl: String)`
  - `R2ConfigParser.parse(jsonString: String): Result<ParsedEndpoints>` where `ParsedEndpoints(powerOn: String, powerOff: String, reboot: String)`
  - `DeviceRepository`: `getDevices(): List<DeviceConfig>`, `addDevice(device: DeviceConfig)`, `deleteDevice(id: String)`

- [ ] **Step 1: Write R2ConfigParserTest**
Write tests covering:
- Standard lowercase keys (`poweron`, `poweroff`, `reboot`)
- CamelCase and underscore keys (`powerOn`, `power_off`, `reboot`)
- Missing key failures (returning Failure with clear message)
- Invalid JSON string handling

- [ ] **Step 2: Implement DeviceConfig and R2ConfigParser**
Implement `R2ConfigParser` using `Gson` to traverse key-value pairs flexibly, extracting URLs regardless of letter case or separator.

- [ ] **Step 3: Write DeviceRepositoryTest**
Write tests for saving devices, loading devices, removing devices, and empty storage defaults.

- [ ] **Step 4: Implement DeviceRepository**
Implement repository backed by `SharedPreferences` storing JSON array via `Gson`.

- [ ] **Step 5: Run unit tests**
Execute unit tests using Java/JUnit runner and verify all pass.

---

### Task 3: Network Trigger Client

**Files:**
- Create: `app/src/main/java/com/expapp/trigger/network/TriggerClient.kt`
- Create: `app/src/main/java/com/expapp/trigger/network/TriggerResult.kt`
- Test: `app/src/test/java/com/expapp/trigger/network/TriggerClientTest.kt`

**Interfaces:**
- Consumes: `DeviceConfig`, `ParsedEndpoints` from Task 2
- Produces:
  - `enum class TriggerAction { POWER_ON, POWER_OFF, REBOOT }`
  - `data class TriggerResponse(val isSuccess: Boolean, val statusCode: Int, val message: String)`
  - `TriggerClient.sendTrigger(device: DeviceConfig, action: TriggerAction, callback: (TriggerResponse) -> Unit)`
  - `TriggerClient.fetchR2Config(url: String, callback: (Result<ParsedEndpoints>) -> Unit)`

- [ ] **Step 1: Write TriggerClientTest**
Test request builder verification:
- POST method used
- `X-Secret-Key` header populated with `device.secretKey`
- Request body contains valid JSON `{"node":"..."}`
- Correct URL selected based on `TriggerAction`

- [ ] **Step 2: Implement TriggerClient**
Implement `TriggerClient` with `OkHttpClient` using 15-second timeouts. Send POST with `application/json` media type and headers. Parse response status and body safely.

- [ ] **Step 3: Run TriggerClient unit tests**
Run tests and verify request formatting and callbacks.

---

### Task 4: UI Design System & Reusable Components

**Files:**
- Create: `app/src/main/java/com/expapp/trigger/ui/theme/Color.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/components/HugeActionButton.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/components/ActionConfirmDialog.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/components/StatusBanner.kt`

**Interfaces:**
- Produces:
  - `HugeActionButton(text: String, subtext: String, containerColor: Color, contentColor: Color, icon: ImageVector, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit)`
  - `ActionConfirmDialog(action: TriggerAction, deviceName: String, onConfirm: () -> Unit, onDismiss: () -> Unit)`
  - `StatusBanner(status: String?, isSuccess: Boolean?, onDismiss: () -> Unit)`

- [ ] **Step 1: Define Color palette and Material 3 theme**
Set up high-contrast dark palette with:
- Power On: `#2E7D32` (Deep Green)
- Reboot: `#EF6C00` (Vibrant Amber)
- Power Off: `#C62828` (Crimson Red)
- Background: `#121212`, Surface: `#1E1E1E`

- [ ] **Step 2: Implement HugeActionButton**
Create large tactile button (minimum height 90dp, rounded corners 20dp, bold typography, ripple effect, loading spinner inside button when active).

- [ ] **Step 3: Implement ActionConfirmDialog**
Material 3 alert dialog confirming destructive actions ("Power Off" / "Reboot") with device name, Cancel button, and Confirm button.

- [ ] **Step 4: Implement StatusBanner**
Compact card or banner showing response status code, elapsed time, and clear error / success badge.

---

### Task 5: Horizontal Pager, Device Screen, Add Device Screen & Main Integration

**Files:**
- Create: `app/src/main/java/com/expapp/trigger/ui/screens/DevicePage.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/screens/AddDevicePage.kt`
- Create: `app/src/main/java/com/expapp/trigger/ui/screens/MainScreen.kt`
- Create: `app/src/main/java/com/expapp/trigger/MainActivity.kt`

**Interfaces:**
- Consumes: `DeviceRepository`, `TriggerClient`, UI components from Tasks 2, 3, 4.
- Produces: Fully assembled application with page swiping, device triggering, device creation, and deletion.

- [ ] **Step 1: Implement DevicePage**
Shows:
- Top bar with Device Name, Node badge, and Delete button (with confirmation)
- Three stacked `HugeActionButton`s: Power On, Reboot, Power Off
- StatusBanner for recent action outcome
- Wire clicks: Power On triggers immediately; Reboot & Power Off open `ActionConfirmDialog` before dispatching.

- [ ] **Step 2: Implement AddDevicePage**
Card with text fields:
- Device Name (e.g., "Home Debian")
- Node identifier (e.g., "debian")
- Cloudflare R2 JSON URL (with paste button)
- Secret Key (with hide/show password toggle)
- "Fetch & Add Device" button that calls `TriggerClient.fetchR2Config`, parses endpoints, creates `DeviceConfig`, saves via `DeviceRepository`, and notifies parent to scroll to new device.

- [ ] **Step 3: Implement MainScreen with HorizontalPager**
- Use `HorizontalPager` with `pageCount = devices.size + 1`.
- Display dots indicator at bottom.
- When no devices exist, page 0 is directly the Add Device screen with friendly onboarding copy.

- [ ] **Step 4: Implement MainActivity**
Set up edge-to-edge window insets, instantiate repository and client, provide them to `MainScreen`.

---

### Task 6: Verification, Compilation Check & User Guide

**Files:**
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `README.md` in `Desktop/Vaishno_workspace/experiments/exp-app/`

- [ ] **Step 1: Create Android resource XMLs**
Provide app name `"Device Trigger"`, default strings, and theme definitions in `app/src/main/res/values/`.

- [ ] **Step 2: Run all unit tests and verify codebase integrity**
Verify all classes compile cleanly and unit tests pass.

- [ ] **Step 3: Write comprehensive README.md**
Document:
- App purpose & features
- How to open and run in Android Studio
- Cloudflare R2 JSON format example
- Server HTTP request specifications (headers, body)
- Adding multiple devices and swiping between them

- [ ] **Step 4: Final verification and summary**
Ensure all files in `Desktop/Vaishno_workspace/experiments/exp-app` are in place and clean.
