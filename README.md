<div align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" width="100" />
  <h1>🛡️ Privacy Guardian</h1>
  <p><b>Next-Generation On-Device Privacy Shield — Built for the iQOO Hackathon</b></p>
</div>

## 🚀 Overview

**Privacy Guardian** is a high-performance, on-device security application designed to protect users from modern data leaks, clipboard snooping, and accidental doxxing. Built specifically with gamers, streamers, and power users in mind, the app leverages local Machine Learning (OCR) to detect and neutralize sensitive information *before* it leaves the device.

By utilizing local processing (simulating the power of the **Snapdragon NPU on the iQOO 15**), Privacy Guardian guarantees **Zero-Cloud Architecture**. Your data never touches a server.

---

## 🔥 Key Features

### 👻 1. Ghost Mode (Context-Aware Data Poisoning)
Traditional privacy apps simply block trackers. Privacy Guardian goes on the offensive. If a malicious app tries to read your clipboard, or if you accidentally paste sensitive data, **Ghost Mode** intercepts it and injects *fake, mathematically valid data*. 
* **Real Credit Card** ➔ Replaced with a valid (but fake) Test Visa (4111...)
* **AWS Access Key** ➔ Replaced with a randomized AKIA... string
* **Result:** Tracker databases are poisoned with useless junk data, destroying their tracking profiles.

### 🎮 2. Esports Streamer Shield
Mobile streamers frequently leak OTPs, personal emails, and passwords when a notification pops up during a live game. The **Streamer Shield** dynamically analyzes incoming text via the NPU and redacts sensitive tokens before they render on screen, ensuring your audience only sees [REDACTED FOR STREAM].

### 📍 3. Location Scrubber (EXIF Stripper)
Sharing a screenshot or photo often leaks your exact GPS coordinates via hidden EXIF metadata. Privacy Guardian's Image Scanner aggressively scrubs TAG_GPS_LATITUDE and longitude data from your media before handing it off to the Android Share Sheet, protecting your physical privacy.

### 👁️ 4. ML Vision (Optical Character Recognition)
Takes an image or screenshot and uses Google ML Kit to extract every line of text. The proprietary **Risk Engine** then scores the text for API Keys, JWTs, Passwords, and Bank Cards, drawing high-visibility bounding boxes around the threats.

### 🚨 5. Panic Protocol (Hardware Lockdown)
Physical privacy matters. Tapping the **Panic Protocol** button triggers an asynchronous kill-switch that instantly clears the Android Clipboard and completely wipes the local SQLite database of all historical scan logs. 

---

## 🛠️ Technical Architecture

* **UI Framework:** 100% Jetpack Compose (Declarative UI)
* **Language:** Kotlin
* **Machine Learning:** Google ML Kit (Text Recognition API v2)
* **Asynchronous Operations:** Kotlin Coroutines & StateFlow
* **Local Storage:** Room Database (SQLite)
* **Theming:** Custom "iQOO Premium Light" Design System (Zinc White / iQOO Racing Orange)

---

## 📥 Installation & Demo

A pre-compiled APK is available in the root of this repository.

1. Download [PrivacyGuardian-iQOO-Hackathon.apk](./PrivacyGuardian-iQOO-Hackathon.apk) to your Android device.
2. Install the APK (You may need to allow "Install from Unknown Sources").
3. **To Test OCR:** Click "Demo Engine" on the Home Screen to see the NPU risk-scoring in action on a pre-loaded image.
4. **To Test Ghost Mode:** Navigate to the "Active Shields" tab and paste a fake password or API key into the Test Environment.

---
*Developed for the iQOO Hackathon.*