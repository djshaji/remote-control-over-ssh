# Remote Control over SSH

A modern Android application built with **Kotlin** and **Jetpack Compose** designed to transform your smartphone into a customizable remote control for Linux machines. The app leverages secure SSH connections to execute user-defined shell commands via an intuitive, interactive dashboard.

## 🚀 Key Features

- **SSH Profile Management**: Securely store and manage multiple Linux server profiles, including host, port, username, and private key authentication.
- **Customizable Command Mapping**: Customize your remote by mapping UI elements—such as dual sliders and a 5-way D-pad—to specific Linux shell commands using `%val%` placeholders for real-time value injection.
- **Interactive Dashboard**: A real-time remote interface featuring:
    - **Dual Sliders**: Perfect for volume, brightness, or any range-based control.
    - **5-Way D-Pad**: Intuitive directional control with a central Select button.
    - **Dedicated Home/Back Buttons**: For seamless navigation and system control.
- **Persistent Connection Engine**: Powered by the `sshj` library, ensuring low-latency, persistent sessions for immediate command execution.
- **Material 3 Design**: Strictly follows Material Design 3 guidelines with:
    - Vibrant, energetic color schemes.
    - Full support for **Dark and Light modes**.
    - **Edge-to-Edge** display support for an immersive experience.
    - Adaptive app icon matching the app's core function.

## 🛠 Tech Stack

Built using the latest Android development standards and cutting-edge libraries:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation 3 (State-driven, type-safe)
- **Database**: Room (For SSH and Remote profile storage)
- **SSH Protocol**: `sshj` library with Bouncy Castle for modern algorithm support
- **Concurrency**: Kotlin Coroutines & Flow
- **Adaptive UI**: Compose Material Adaptive Library

---
*Developed with a focus on robustness, maintainability, and user-centric design.*
