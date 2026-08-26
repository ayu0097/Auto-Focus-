# 🎬 AutoReframe 9:16 — Intelligent AI Vertical Video Reframer

[![Android CI & Build](https://github.com/your-username/autoreframe-android/actions/workflows/android.yml/badge.svg)](https://github.com/your-username/autoreframe-android/actions/workflows/android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-brightgreen.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%2B%20(Android%208.0)-blue.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> Automatically transform landscape (16:9, 4:3, 21:9) videos into smooth, cinematic 9:16 vertical videos ready for **TikTok, Instagram Reels, and YouTube Shorts** using intelligent computer vision tracking and damped camera smoothing.

---

## ✨ Features

- 🎯 **Smart Subject Tracking**: Dual-pass computer vision engine detecting faces, human silhouettes, and optical motion centroids.
- 🎥 **Cinematic Camera Smoothing**: Dead-zone thresholds and critically damped spring filters simulate a steady, professional human camera operator without jitter.
- 🔍 **Interactive Director Mode**: View the full landscape frame with an active 9:16 bounding box, rule-of-thirds grid, detected subject markers, and tap-to-focus / drag-to-pin controls.
- 📱 **Real-time 9:16 Output Preview**: Instantaneous preview of what your vertical short will look like.
- ⚡ **1-Touch Instant Presets**:
  - 🎬 **Cinematic Smooth**: Subtle, graceful camera moves for interviews and vlogs.
  - ⚡ **Dynamic Action**: Responsive, fast tracking for action sports, skateboarding, and dance.
  - 🎯 **Face Spotlight**: Locked focus on presenter with rule-of-thirds headroom bias.
  - 🔒 **Fixed Center**: Static anchor for symmetrical shots.
- 🚀 **Hardware Transcoding & Muxing**: Fast on-device hardware-accelerated H.264 MP4 export at 1080x1920 (Full HD), 720x1280 (HD), or 1440x2560 (2K QHD) with lossless audio pass-through.
- 📲 **Instant Social Sharing**: Export directly to TikTok, Instagram, YouTube Shorts, WhatsApp, or save to Camera Roll (`MediaStore`).

---

## 🛠️ Architecture & Tech Stack

- **UI & Design**: Jetpack Compose, Material Design 3, Dark Studio Aesthetic with Cyan Neon & Deep Violet accents.
- **Architecture**: MVVM (Model-View-ViewModel), StateFlow, Coroutines.
- **Media Pipeline**: Android `MediaMetadataRetriever`, `MediaExtractor`, `MediaCodec`, `MediaMuxer`.
- **Testing**: Robolectric (JVM-based unit & integration tests), Roborazzi (Screenshot regression testing).

---

## 🚀 Getting Started & Building

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17
- Android SDK 34+

### Clone & Build with Gradle

```bash
# Clone the repository
git clone https://github.com/your-username/autoreframe-android.git
cd autoreframe-android

# Run unit tests
gradle testDebugUnitTest

# Build Debug APK
gradle assembleDebug

# Build Release APK
gradle assembleRelease
```

The compiled APK will be located in:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤖 GitHub Actions CI/CD

This repository is pre-configured with continuous integration workflows in `.github/workflows/`:
1. **`android.yml`**: Automatically runs tests and builds the debug APK on every commit and pull request.
2. **`release.yml`**: Generates and packages production release artifacts when a release tag (`v*`) is pushed.

---

## 📄 License

```
Copyright 2026 AutoReframe Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
