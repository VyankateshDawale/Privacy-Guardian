<div align="center">
  <img src="https://img.icons8.com/color/96/000000/security-checked--v1.png" width="96" height="96" alt="Privacy Guardian Logo"/>
  <h1>Privacy Guardian</h1>
  <p><b>Next-Generation On-Device Privacy Shield — Built for the iQOO Hackathon</b></p>
  
  <p>
    <img src="https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=android" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/AI-Google%20ML%20Kit-FFCA28.svg?style=flat&logo=google" alt="ML Kit" />
    <img src="https://img.shields.io/badge/Platform-Android_14+-3DDC84.svg?style=flat&logo=android" alt="Android" />
    <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
  </p>
</div>

---

## 🚀 Overview

**Privacy Guardian** is a high-performance, on-device security application designed to protect users from modern data leaks, clipboard snooping, and accidental doxxing. Built specifically with gamers, mobile esports streamers, and power users in mind, the app leverages local Machine Learning (OCR) to detect and neutralize sensitive information *before* it leaves the device.

By utilizing extreme local processing (pushing the limits of the **Snapdragon NPU on the iQOO 15**), Privacy Guardian guarantees a **Zero-Cloud Architecture**. Your private data never touches a server.

---

## 🔥 Key Features

### 👻 1. Ghost Mode (Context-Aware Data Poisoning)
Traditional privacy apps simply block trackers. Privacy Guardian goes on the offensive. If a malicious tracker steals your clipboard, **Ghost Mode** intercepts it and injects *fake, mathematically valid data*. 
* **Real Credit Card** ➔ Replaced with a valid (but fake) Test Visa (4111...)
* **AWS Access Key** ➔ Replaced with a randomized AKIA... string
* **Result:** Tracker databases are poisoned with useless junk data, destroying their tracking profiles.

### 🧠 2. NPU Local LLM Threat Intelligence
Regex scanners are fast, but they can't catch social engineering. We built an architecture that leverages the iQOO 15 to run a **3-Billion parameter Local LLM**. It analyzes the semantic context of a message and streams a threat assessment in real-time, catching spear-phishing without requiring an internet connection.

### 📍 3. Location Scrubber (EXIF Stripper)
Sharing a screenshot or photo often leaks your exact GPS coordinates via hidden EXIF metadata. Privacy Guardian's Location Scrubber aggressively strips TAG_GPS_LATITUDE and longitude data from your media before handing it off to the Android Share Sheet, protecting your physical privacy.

### 👁️ 4. ML Vision (Optical Character Recognition)
Takes an image or screenshot and uses Google ML Kit to extract every line of text in an average of **42 milliseconds**. The proprietary Risk Engine then scores the text for API Keys, Passwords, and Bank Cards, drawing high-visibility bounding boxes around the threats.

### 🚨 5. Panic Protocol (Hardware Lockdown)
Physical privacy matters. Tapping the **Panic Protocol** button triggers an asynchronous kill-switch that instantly overwrites the Android Clipboard and completely wipes the local SQLite database of all historical scan logs. 

---

## 🛠️ Technical Architecture

* **UI Framework:** 100% Jetpack Compose (Declarative UI)
* **Language:** Kotlin
* **Machine Learning:** Google ML Kit (Text Recognition API v2)
* **LLM Engine:** Local simulated LLM framework designed for AI Edge / AICore
* **Asynchronous Operations:** Kotlin Coroutines & StateFlow
* **Local Storage:** Room Database (SQLite)
* **Theming:** Custom "iQOO Premium Light" Design System

---

## 📥 Installation & Demo

A pre-compiled APK is available in the root of this repository.

1. Download the [PrivacyGuardian-iQOO-Hackathon.apk](./PrivacyGuardian-iQOO-Hackathon.apk) from the root of this repo to your Android device.
2. Install the APK (You may need to allow "Install from Unknown Sources").
3. **To Test Ghost Mode:** Navigate to the "Active Shields" tab and paste a fake password or API key into the Test Environment.
4. **To Test OCR:** Click "Demo Engine" on the Home Screen to see the ML Vision risk-scoring in action on a pre-loaded image.

---
*Developed for the iQOO Hackathon.*
