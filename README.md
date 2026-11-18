# Device Trigger — Android Remote Device Control App

A clean, native Android mobile application designed to remotely control the power states of multiple machines (e.g. servers, Debian/Pop!_OS nodes, lab rigs).

The app features a **horizontal swipe interface** where each device is represented by a dedicated screen with **three massive, high-contrast, tactile buttons**:
1. 🟢 **POWER ON** — Sends remote power on / wake trigger
2. 🟠 **REBOOT** — Gracefully restarts the remote machine (confirmation protected)
3. 🔴 **POWER OFF** — Shuts down the machine (confirmation protected)

Endpoints for each machine are loaded dynamically by fetching a lightweight JSON configuration hosted in a **Cloudflare R2** bucket. Critical actions (`poweroff` and `reboot`) authenticate using a device-specific `secretKey`.

---

## 📱 Pre-built Ready-to-Install APK

The application has been compiled into a ready-to-install debug APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Installing on your Android phone via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*Or transfer `app-debug.apk` directly to your phone and tap to install.*

---

## 🚀 Key Features

- **Massive Tactile Buttons**: High-contrast, large touch targets designed to prevent accidental mis-taps while remaining effortless to press.
- **Horizontal Swipe Navigation**: Seamlessly swipe left and right between configured machines with an animated bottom page indicator.
- **Dynamic Cloudflare R2 Configuration**: Simply supply your Cloudflare R2 JSON URL when adding a device; the app fetches and validates endpoint URLs automatically.
- **Multi-Device Support**: Manage any number of remote machines. The last swipe tab is always an **"+ Add Device"** configuration screen.
- **Persistent Local Storage**: Configured machines are safely saved to Android persistent storage (`SharedPreferences`) so they remain available whenever the app is reopened.
- **Mistake-Proof Safety Prompts**: Reboot and Power Off actions trigger a quick confirmation dialog before sending the request.
- **Instant Visual Feedback**: On-screen status banners, in-button loading indicators, and Android Toast notifications display HTTP status codes (e.g., `200 OK`) and server responses.

---

## ⚙️ Cloudflare R2 JSON Specification

When adding a device, the application downloads and parses a JSON file hosted in your Cloudflare R2 bucket.

### Example JSON file:
```json
{
  "poweron": "https://cosmos.linuxguy.space/poweron",
  "poweroff": "https://cosmos.linuxguy.space/poweroff",
  "reboot": "https://cosmos.linuxguy.space/reboot"
}
```

*Note: The built-in parser is resilient and automatically accepts key variants such as `power_on`, `powerOn`, `power_off`, `powerOff`, `restart`, and `shutdown`.*

---

## 📡 API Request Specification

When an action is triggered, the app dispatches an asynchronous HTTP `POST` request using `OkHttp`:

| Field | Value / Description |
|---|---|
| **HTTP Method** | `POST` |
| **Header 1** | `Content-Type: application/json` |
| **Header 2** | `X-Secret-Key: <your_secretKey>` (e.g., `Carbon@12`) |
| **JSON Body** | `{"node": "<node_identifier>"}` (e.g., `{"node": "debian"}`) |
| **Timeout** | 15 seconds connect / read / write |

---

## 🛠️ Project Structure

```
exp-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/expapp/trigger/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/DeviceConfig.kt
│   │   │   │   │   ├── parser/R2ConfigParser.kt
│   │   │   │   │   └── repository/DeviceRepository.kt
│   │   │   │   ├── network/
│   │   │   │   │   ├── TriggerClient.kt
│   │   │   │   │   └── TriggerResult.kt
│   │   │   │   └── ui/
│   │   │   │       ├── components/
│   │   │   │       │   ├── ActionConfirmDialog.kt
│   │   │   │       │   ├── HugeActionButton.kt
│   │   │   │       │   └── StatusBanner.kt
│   │   │   │       ├── screens/
│   │   │   │       │   ├── AddDevicePage.kt
│   │   │   │       │   ├── DevicePage.kt
│   │   │   │       │   └── MainScreen.kt
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt
│   │   │   │           └── Theme.kt
│   │   │   └── res/
│   │   │       └── values/
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   │   └── test/java/com/expapp/trigger/
│   │       ├── data/parser/R2ConfigParserTest.kt
│   │       ├── data/repository/DeviceRepositoryTest.kt
│   │       └── network/TriggerClientTest.kt
│   └── build.gradle.kts
├── docs/superpowers/
│   ├── specs/2026-11-18-remote-device-trigger-app-design.md
│   └── plans/2026-11-18-remote-device-trigger-app.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

---

## 💻 Building from Source

### Prerequisites:
- Java JDK 17 or 21
- Android SDK (API 34)

### Run Unit Tests:
```bash
./gradlew testDebugUnitTest
```

### Build Debug APK:
```bash
./gradlew assembleDebug
```
The generated APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Open in Android Studio:
1. Open Android Studio.
2. Select **File -> Open...** and navigate to `/home/creserver/Desktop/Vaishno_workspace/experiments/exp-app`.
3. Allow Gradle sync to complete and click **Run** (Shift+F10) on a connected device or emulator.
