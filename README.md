# BlueOS Widgets Port ✦ Kivotos Edition

A clean, lightweight, standalone port of custom BlueOS / Kivotos widgets for any Android 12+ device (HyperOS, MIUI, AOSP, OneUI, Pixel OS, ColorOS, etc.).

No heavy frameworks, no bloated background services — pure native Android RemoteViews, high battery efficiency, and authentic Kivotos aesthetic.

---

## ✨ Features & Included Widgets

Every widget family is available in **two separate styles**:
1. **Card plate** — Dark filled background with subtle 1.2dp border, supporting dynamic theme colors (Pure Black, AOSP Dark, Monet, Slate) and alpha slider.
2. **Transparent** — Completely borderless and transparent (`0% background, 0% outline`), cleanly floating directly over your wallpaper.

### 🕒 Clocks
* **Kivotos Moe Clock (2x2)** — Floating Kivotos anime character digits (`0.png`–`9.png`) with exact minute-by-minute synchronization.
* **Halo Clock (2x2)** — Clean digital typography with the iconic BlueOS halo hovering over minutes.
* **Card Clock (2x2 & 4x2 Wide)** — Big bold time, localized date, and next alarm status.
* **Tile Clock (2x2)** — Stacked hours over minutes under the Kivotos halo.
* **Next Alarm (2x2)** — Big alarm time with relative countdown ("Tomorrow morning").

### 🎵 Media & Live Equalizers
* **Retro Walkman Cassette (4x2)** — Classic cassette tape player with turning reels and track info.
* **Vinyl Player (2x2)** — Spinning vinyl record with centered album artwork.
* **Square (2x2) & Wide Bar (4x1 / 4x2)** — Full album art, track title, artist, progress bar, and playback controls.
* **Live Equalizers (React to music playback in real-time):**
  * **Bars (4x1)** — Animated spectrum frequency bars.
  * **Wave (4x1)** — Flowing dual-frequency sine soundwaves.
  * **Ring (2x2)** — Circular radar acoustic pulse around album art.
  * **Full Player (4x2)** — Complete player with integrated real-time visualizer.
* *Strictly reactive audio session listener — completely eliminates phantom autoplay or accidental resume bugs.*

### 📅 Date, Calendar & Events
* **Month Calendar Grid (4x2)** — Full 7×6 calendar month view with day-of-week header and current day highlighted.
* **Schedule & Events (4x2)** — Today's date with upcoming calendar events.
* **Countdown (2x2)** — Days left until any event you choose (e.g., Birthday, New Year).

### 🤖 Schale OS Desktop Companions (Bob to music beat!)
* **Arona (4x2)** — Interactive Arona with situational dialogue and music rhythm swaying.
* **Plana (4x2)** — Plana companion keeping tactical watch.
* **Duo: Arona & Plana (4x2)** — Both companions on your home screen together.

### ⚡ System & Quick Utilities
* **System Monitor (4x2)** — Real-time Battery %, RAM usage (GB), and Internal Storage %.
* **Battery Rings (1x1, 2x2, 4x2)** — Circular battery gauges with phone charge state and connected Bluetooth headset battery.
* **Quick Toggles (4x1)** — Flashlight, Do Not Disturb, Auto-rotate, Dark Theme, and Battery Saver.
* **Photo / Sticker Widgets** — Rounded rectangle, circle, and borderless sticker frames.

---

## 🎨 System Widget Picker Previews
All 58 widgets include native raster `previewImage` and `previewLayout` resources. In HyperOS / MIUI and modern AOSP launchers, every widget displays an authentic, high-resolution preview directly in the system widget picker.

## 🌐 Localization
* Built-in support for **Russian** and **English**.
* First-launch onboarding dialog allows instant language selection.
* Explicit language switch in the app settings that persists across reboots.

---

## 🛠️ How to Build on PC

You don't need Android Studio or Gradle. You just need standard JDK and the Android SDK command-line tools.

### Prerequisites
* **Java Development Kit (JDK) 17+** (`javac`, `keytool`)
* **Android SDK** with:
  * Platform: `android-34` (or any `android-31` to `android-34`)
  * Build-Tools: `34.0.0` or higher (`aapt`, `d8`, `apksigner`)

### Quick Build:

#### On Linux / macOS / WSL:
```bash
# 1. Clone repository
git clone https://github.com/Xlufoi/BlueOS-widgets-port.git
cd BlueOS-widgets-port

# 2. Make build script executable and run
chmod +x build.sh
./build.sh
```

#### On Windows (Command Prompt):
```cmd
git clone https://github.com/Xlufoi/BlueOS-widgets-port.git
cd BlueOS-widgets-port
build.bat
```

> **Note:** If `debug.keystore` is not found, the build script will automatically generate one for you.
>
> Once finished, your signed ready-to-install APK will be in:
> ```
> out/BlueOS-Widgets.apk
> ```

---

## 📱 Download Ready APK
Grab the pre-built, signed APK directly from the [Releases](https://github.com/Xlufoi/BlueOS-widgets-port/releases) section.

---

## 📜 License
Distributed under the MIT License. Blue Archive assets and inspirations belong to NEXON Games & Yostar.
