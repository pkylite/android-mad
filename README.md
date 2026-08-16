# 📱 Android Mobile Application Development Labs

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)](https://kotlinlang.org/)
[![Android Studio](https://img.shields.io/badge/IDE-Android%20Studio-green?logo=android-studio)](https://developer.android.com/studio)

Welcome to the official repository for the **Mobile Application Development** course labs. This repository contains the starter code, completed reference solutions, and step-by-step guides for building modern, production-ready Android applications using Kotlin and Jetpack Compose.

## 🎯 Course Objectives

By completing these labs, you will gain hands-on experience with:
-   Building declarative UIs with/without **Jetpack Compose**.
-   Implementing modern **Android Architecture Components** (ViewModel, StateFlow).
-   Managing **Asynchronous Programming** (Kotlin Coroutines & Flows).
-   Integrating with **System APIs** (Permissions, Camera, Location, Speech).
-   Handling **Networking** (Retrofit) and **Local Persistence** (Room, DataStore).
-   Implementing **Background Work** (WorkManager, AlarmManager) and **Security** (Secure token auth).

---

## 📚 Lab Index & Recommended Path

While all labs are available in this repository, they are designed to build upon each other. We highly recommend following the **Optimized Learning Path** below, which sequences the labs to introduce concepts progressively without overwhelming you with redundancy.

### The Optimized Learning Path

| Sequence | Lab | Primary Concepts Introduced | Difficulty |
| :--- | :--- | :--- | :--- |
| **1** | **[Lab 1: Calculator App](./Lab01_Calculator)** | UI Layout (Compose), State Management, Event Handling | ![Beginner](https://img.shields.io/badge/Difficulty-Beginner-green) |
| **2** | **Lab 2: VUI Enabled App** | Runtime Permissions, System Services, Async Callbacks | ![Beginner](https://img.shields.io/badge/Difficulty-Beginner-green) |
| **3** | **Lab 4: Weather App** | REST APIs, Retrofit, JSON Parsing, Coroutines | ![Intermediate](https://img.shields.io/badge/Difficulty-Intermediate-yellow) |
| **4** | **Lab 7: Login System** | Secure Storage, Retrofit Interceptors, Token Refresh | ![Intermediate](https://img.shields.io/badge/Difficulty-Intermediate-yellow) |


### Supplementary / Elective Labs
#### Elective
| Sequence | Lab | Primary Concepts Introduced | Difficulty |
| :--- | :--- | :--- | :--- |
| **5** | **Lab 3: Note-Taking App** | Room Database, Offline-First Architecture, Firebase Sync | ![Advanced](https://img.shields.io/badge/Difficulty-Advanced-red) |
| **6** | **Lab 5: Reminders & Notifications** | AlarmManager, Notifications, BroadcastReceivers | ![Advanced](https://img.shields.io/badge/Difficulty-Advanced-red) |
| **7** | **Lab 6: Location Tracking** | FusedLocationProvider, Background Location, EXIF Geotagging | ![Advanced](https://img.shields.io/badge/Difficulty-Advanced-red) |

These labs cover specific hardware integrations and background concepts. They are excellent for final projects or extra credit, but cover overlapping architectural patterns with the core path above.
#### Supplementary
-   **Lab: Background Service & Widget**: WorkManager, App Widgets, Periodic Updates.
-   **Lab: Media Player / Camera**: CameraX, ExoPlayer, Complex Hardware State.

*(Note: Lab 0 (Setup and Hello World!) is intentionally omitted from this curriculum).*

---

## 🛠️ Tech Stack & Prerequisites

Before starting, ensure you have the following set up on your development machine:

1.  **Android Studio Hedgehog (2023.1.1)** or newer.
2.  **Android SDK** with API Level 34 (Android 14) installed.
3.  Basic understanding of **Kotlin** syntax (classes, lambdas, null safety).
4.  A physical Android device is **highly recommended** for Labs 3, 7, 8, and 9 (Emulators often lack proper support for Speech, Camera, and precise Location).

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/pkylite/android-mad.git
cd android-mad
```

### 2. Open a Lab in Android Studio
1.  Open Android Studio.
2.  Select **File > Open**.
3.  Navigate to the cloned repository and select the specific lab folder (e.g., `Lab01_Calculator`).
4.  Wait for Gradle sync to complete (this may take a few minutes the first time).

### 3. Branch Strategy (For Students)
-   The `main` branch contains starter code and reference solutions.
-   Create your own branch to complete your work:
    ```bash
    git checkout -b <your-name>-lab01
    ```
-   **Commit often!** Android Studio has built-in Git integration (Commit via `VCS > Commit`).

---

## 📂 Repository Structure

Each lab should follow a consistent folder structure to make navigation easy:

```text
LabXX_Name/
├── app/                  # Main application module
│   ├── src/main/
│   │   ├── java/.../     # Kotlin source code
│   │   ├── res/          # Resources (layouts, drawables, values)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts  # App-level dependencies
├── gradle/               # Gradle wrapper files
└── README.md             # Specific instructions for this lab
```

Inside each lab's specific `.md`, you will find:
-   **Lab Overview:** What you are building.
-   **Step-by-Step Guide:** Detailed instructions if you are starting from the starter code.
-   **Key Concepts:** Explanation of the Android APIs being used.
-   **Testing Checklist:** How to verify your app works correctly (Optional).

---

## 🆘 Troubleshooting & FAQ

<details>
<summary><strong>Gradle Sync Failed / SDK Issues</strong></summary>

1. Ensure you have an internet connection for the first sync to download dependencies.
2. Go to **File > Project Structure > SDK Location** and ensure the Android SDK path is correctly set.
3. Go to **Tools > SDK Manager** and ensure SDK Platform 34 and the Android SDK Build-Tools 34 are installed.
</details>

<details>
<summary><strong>Emulator Won't Start / HAXM Error</strong></summary>

If you are on Windows and see an HAXM error, you need to enable hardware acceleration:
1. Open BIOS/UEFI settings and enable Virtualization (Intel VT-x or AMD-V).
2. Reinstall Intel HAXM via the Android SDK Manager.
</details>

<details>
<summary><strong>Permissions Denial on Modern Android (13+)</strong></summary>

If your app crashes when trying to access the Camera, Location, or Notifications, it is likely because you did not implement **Runtime Permissions**. Android no longer allows permissions to be granted just by listing them in the Manifest. You must ask the user at runtime. Labs 2 and 6 cover this extensively.
</details>

---

## 📜 License

This educational material is provided for students of the Mobile Application Development course. Please do not distribute solutions publicly.

Happy Coding! 🚀
