# YEME BINGO (የሜ ቢንጎ) 🇪🇹🎱

A modern, luxury Ethiopian 75-Ball Bingo game built with **Jetpack Compose**, **Kotlin Coroutines**, and **Material Design 3**. Designed specifically for Ethiopian players with native **Amharic voice calling**, 17 distinct winning patterns, up to 500 cartelas, and integrated **Telebirr payment gateway** with automatic receipt verification and anti-replay protection.

---

## 📱 Features

- **Native Amharic Voice Engine**:
  - Pure Amharic phonetic numerals (e.g. *ቢ አስራ አምስት*, *አይ ሃያ ስምንት*, *ኤን አርባ ሁለት*, *ጂ ሃምሳ ስድስት*, *ኦ ሰባ አራት*).
  - High-fidelity synthetic audio synthesis using standard Android AudioTrack PCM waveforms combined with Android TTS fallback for crystal-clear caller audio.
  - Dual language toggle: Amharic (አማርኛ) and English.
  
- **75-Ball Luxury Bingo Engine**:
  - B (1–15), I (16–30), N (31–45), G (46–60), O (61–75).
  - 17 Winning Patterns: Single Line (Horizontal/Vertical/Diagonal), 4 Corners, Letter X, Letter T, Letter L, Plus (+), Postage Stamp, Diamond, Full House, and more.
  - Multi-cartela support: Play with 1, 2, 4, or up to 500 cartelas with instant auto-daub and win verification.
  - Practice / Offline Solo Mode and Scheduled Live Multiplayer Room.

- **Telebirr Payment Gateway**:
  - Native Telebirr checkout dialog with automated QR / payment intent generation.
  - **Account Holder**: `0939383425` (*Yemane*).
  - **Cartela Cost**: 30 ETB per cartela (Minimum deposit: 30 ETB).
  - **Direct USSD**: Quick launch `*127#` for instant dialer checkout.
  - **Automated Electronic Receipt Verification**:
    - Live transaction verification with Ethio Telecom electronic receipt gateway (`https://transactioninfo.ethiotelecom.et/receipt/<txId>`).
    - Recipient name and phone validation ensures transfers are credited to the verified account holder.
    - Anti-replay system checks redeemed receipts to prevent double-spending.
  - **Enhanced Paste Experience**: Long-press any input space or tap the clipboard icon to paste Telebirr transaction URLs or IDs instantly.

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Architecture**: Clean MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF)
- **Asynchronous**: Kotlin Coroutines & `StateFlow`
- **Audio**: Custom Amharic Audio Synthesizer (PCM wavetable & Android TTS)
- **Networking**: OkHttp 4.12+ & Retrofit for Telebirr transaction verification
- **Persistence**: Room Database & SharedPreferences anti-replay vault
- **Security**: HMAC-SHA256 signature verification & Ethio Telecom HTML receipt parsing

---

## 🚀 Building the Project

### Prerequisites
- Android Studio Ladybug / Meerkat (or JDK 17 / 21)
- Android SDK 34+ (targetSdk 36, minSdk 24)

### Clone & Build
```bash
git clone https://github.com/<your-username>/yeme-bingo.git
cd yeme-bingo
```

### Build Debug APK
```bash
gradle assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
gradle assembleRelease
```
The signed release APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## ⚡ GitHub Actions CI/CD

This repository includes a ready-to-use GitHub Actions workflow configured in `.github/workflows/android.yml`:

- **Automatic Builds**: Triggers on every push to `main` or `master` branches and pull requests.
- **Manual Trigger**: Go to the **Actions** tab on GitHub, select **Android CI & APK Build**, and click **Run workflow** to build anytime.
- **Artifacts**: Every workflow run generates and uploads `YemeBingo-Debug-APK` directly under the **Artifacts** section of the Actions run summary.
- **Automatic Releases**: Pushing any tag starting with `v` (e.g. `v1.0.0`) automatically creates a GitHub Release with the compiled APK attached.

---

## 📦 How to Publish to GitHub Releases

1. **Push your code to GitHub**:
   ```bash
   git init
   git add .
   git commit -m "Initial release of YEME BINGO v1.0.0"
   git branch -M main
   git remote add origin https://github.com/<your-username>/yeme-bingo.git
   git push -u origin main
   ```

2. **Create a GitHub Release**:
   - Go to your GitHub repository -> **Releases** -> **Draft a new release**.
   - Create a tag, e.g., `v1.0.0`.
   - Title: `YEME BINGO v1.0.0 - Official Android Release`.
   - Drag and drop `app/build/outputs/apk/debug/app-debug.apk` (or `app-release.apk`) into the **Attach binaries** section.
   - Click **Publish release**.

3. **Install APK on Android Device**:
   - Download the `.apk` file onto your Android device.
   - Tap the APK file and select **Install** (allow "Install unknown apps" if prompted).
   - Launch **YEME BINGO** and enjoy!

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
