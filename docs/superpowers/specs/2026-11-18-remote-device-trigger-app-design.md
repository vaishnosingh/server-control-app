# Remote Device Trigger Android Application Design

## 1. Intent and Context
The user needs a dedicated mobile trigger application on Android to manage and control multiple remote machines (e.g., home lab servers, remote Debian or Pop!_OS nodes). The application provides an immediate, mistake-resistant control interface featuring three large buttons for each device: **Power On**, **Power Off**, and **Reboot**.

Devices are organized in a horizontal swipe interface. Device configuration URLs are fetched dynamically from a JSON file hosted in a Cloudflare R2 bucket, and critical commands (Power Off, Reboot) authenticate using a device-specific `secretKey`.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **Clear, Large Touch Targets**: Three huge, high-contrast, color-coded buttons (Power On, Power Off, Reboot) that are easy to tap accurately on a smartphone.
- **Multi-Device Horizontal Swiping**: A smooth horizontal pager allowing users to swipe left/right between devices, with visible page indicators.
- **Dynamic Configuration via Cloudflare R2**: Dedicated "Add Device" tab/card that takes a Cloudflare R2 JSON URL, fetches the endpoint mapping (`poweron`, `poweroff`, `reboot`), and configures the device.
- **Secure Request Dispatch**: Send HTTP POST requests using OkHttp with the `X-Secret-Key` header and JSON body `{"node": "<node>"}`.
- **Reliable Local Persistence**: All added devices and their configurations persist locally across application launches using `SharedPreferences`.
- **Immediate User Feedback**: Visual feedback for in-flight requests (loading indicators) and clear Toast / snackbar messages reporting HTTP status (e.g., `200 OK`, `Shutting down...`, or error details).

### Non-Goals
- Complex user account login or cloud synchronization across multiple phones (configurations are stored locally on the device).
- Real-time device telemetry / hardware sensor monitoring (CPU/RAM graphs); this is strictly a power-state remote trigger.

### Anti-Goals
- Cluttered, small text-heavy UI that makes hitting power buttons difficult or error-prone.
- Blocking the UI thread during network calls.
- Hardcoding endpoints and secret keys inside the application binary.

---

## 3. Constraints
- **Platform**: Android (minSdk 24, targetSdk 34) ensuring wide compatibility with virtually all modern Android phones.
- **Directory**: Must be placed in `/home/creserver/Desktop/Vaishno_workspace/experiments/exp-app`.
- **Language & UI Toolkit**: Kotlin with modern Jetpack Compose and Material 3.
- **Networking**: OkHttp for asynchronous HTTP requests and robust header/timeout handling.
- **Endpoint Structure**: Cloudflare R2 JSON contains URLs keyed by action (handling `poweron`, `poweroff`, `reboot` with flexible key parsing).
- **Authentication**: `X-Secret-Key` header passed with Power Off and Reboot requests, plus JSON body payload containing the node identifier and secret key.

---

## 4. Approaches Considered

1. **Modern Jetpack Compose Native App (Selected)**
   - *Architecture*: Single Activity using Jetpack Compose with `HorizontalPager`, Material 3 components, OkHttp, and Gson.
   - *Why chosen*: Cleanest declarative code, zero XML layout bloat, modern touch animations, responsive layouts with custom huge buttons, and straightforward state management.
2. **Classic ViewPager2 + XML Layouts**
   - *Why rejected*: Significant boilerplate across XML layouts, RecyclerView adapters, and ViewBinding.
3. **Android WebView Hybrid**
   - *Why rejected*: Unnecessary web engine overhead, less responsive touch/swipe feel, and inferior native integration.

---

## 5. Architectural and Behavioral Design

### 5.1 Navigation & Screen Flow
- Uses `HorizontalPager` with `n + 1` pages:
  - Pages `0` to `n - 1`: Individual configured device trigger cards.
  - Page `n`: Dedicated "+ Add Device" card.
- A horizontal page indicator (dots) at the bottom or top of the screen displays the current position and total pages.
- Header contains an app bar with the application title ("Device Trigger") and an option to delete/edit the currently selected device.

### 5.2 Device Screen (Pages 0 .. n-1)
- **Header**: Device Name (large bold font) and Node tag (e.g. `debian`).
- **Control Buttons**:
  - **Power On** (Green `#2E7D32`): Sends request to `poweron` URL.
  - **Reboot** (Amber/Orange `#EF6C00`): Prompts a quick confirmation sheet/dialog ("Are you sure you want to reboot [Device]?") -> on confirm, sends POST request to `reboot` URL with `X-Secret-Key`.
  - **Power Off** (Red `#C62828`): Prompts a quick confirmation sheet/dialog ("Are you sure you want to power off [Device]?") -> on confirm, sends POST request to `poweroff` URL with `X-Secret-Key`.
- **Status Display**: A compact status card showing last action result (timestamp, response code, message) and loading progress bar when a request is active.

### 5.3 Add Device Screen (Last Page)
- Form fields:
  - **Device Name** (e.g., "Home Server Debian")
  - **Node Identifier** (e.g., "debian" or "pop-os")
  - **Cloudflare R2 JSON URL** (e.g., `https://.../endpoints.json`)
  - **Secret Key** (e.g., `Carbon@12` - with show/hide password toggle)
- Actions:
  - **Fetch & Save**:
    1. Downloads JSON from the provided Cloudflare R2 URL.
    2. Validates that required action URLs exist (`poweroff`, `reboot`, and `poweron`).
    3. Saves device to local persistent storage.
    4. Automatically scrolls to the newly created device tab.

### 5.4 Data Storage & Models
- `DeviceConfig`:
  - `id: String` (UUID)
  - `name: String`
  - `node: String`
  - `secretKey: String`
  - `r2Url: String`
  - `powerOnUrl: String`
  - `powerOffUrl: String`
  - `rebootUrl: String`
- `DeviceRepository`:
  - Serializes and stores `List<DeviceConfig>` in `SharedPreferences` via Gson.
  - Provides reactive State / Flow of devices.

### 5.5 Cloudflare R2 JSON Format
The expected JSON format fetched from Cloudflare R2:
```json
{
  "poweron": "https://cosmos.linuxguy.space/poweron",
  "poweroff": "https://cosmos.linuxguy.space/poweroff",
  "reboot": "https://cosmos.linuxguy.space/reboot"
}
```
*Note: Parser is resilient to key case variations (`powerOn`, `power_on`, `poweroff`, etc.).*

### 5.6 Network Request Specification
- **Client**: `OkHttpClient` with 15-second connect/read timeouts.
- **Power Off, Reboot, and Power On**:
  - Method: `POST`
  - Headers:
    - `Content-Type: application/json`
    - `X-Secret-Key: <device.secretKey>`
  - Body:
    ```json
    {
      "node": "<device.node>"
    }
    ```
  - Both header (`X-Secret-Key`) and body (`{"node": "..."}`) match the server configuration and snippet provided.

---

## 6. What Is Left to the Builder
- Visual styling polish: button elevation, corner radius (16dp), vibrant Material3 color theming, and dark mode support.
- Optional preloaded demo/sample device for first-time onboarding if storage is empty.
- Gradle build and wrapper configuration to ensure clean standalone compilation.
