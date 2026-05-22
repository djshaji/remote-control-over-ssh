# Project Plan

Build a customizable SSH remote control app for Linux machines using Kotlin and Jetpack Compose.

## Project Brief

# Project Brief: SSH Remote Control

A robust, Material 3-based Android application designed to transform your smartphone into a customizable remote control for Linux machines. The app leverages secure SSH connections to execute user-defined shell commands via an intuitive, interactive dashboard.

### Features
*   **SSH Profile Management:** Securely store and manage multiple Linux server profiles, including support for host, port, username, and internal private key storage.
*   **Dynamic Command Mapping:** Customize your remote by mapping UI elements—such as dual sliders and a 5-way D-pad—to specific Linux shell commands using `%val%` placeholders.
*   **Interactive Control Dashboard:** A real-time remote interface featuring two debounced sliders, a full 5-way directional pad, and dedicated Home/Back buttons for seamless navigation.
*   **Persistent Connection Engine:** A background SSH manager (powered by `sshj`) that maintains a persistent, low-latency session for immediate command execution.

### High-Level Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Navigation:** Jetpack Navigation 3 (State-driven)
*   **Adaptive Strategy:** Compose Material Adaptive Library (Supporting various screen sizes and foldables)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Persistence:** Room Database (For SSH and Remote profile storage)
*   **SSH Protocol:** `sshj` library

### UI Design Image
![UI Design](/home/djshaji/AndroidStudioProjects/remotecontroloverssh/input_images/image_0.png)
Image path = /home/djshaji/AndroidStudioProjects/remotecontroloverssh/input_images/image_0.png

## Implementation Steps

### Task_1_Setup_Data_SSH_Service: Configure Room database for SSH and Remote profiles. Implement the SSH engine using 'sshj' to manage persistent sessions and execute shell commands. Add necessary dependencies to libs.versions.toml.
- **Status:** COMPLETED
- **Updates:** Added sshj, bouncycastle, and slf4j dependencies. Implemented SshProfile and RemoteCommand Room entities/DAOs. Created AppDatabase. Developed SshManager for persistent SSH sessions. Created KeyFileHelper for private key management. Registered BouncyCastle provider in Application class. Updated SDK versions to 37.
- **Acceptance Criteria:**
  - Room entities and DAOs for SSH profiles and Remote profiles are implemented.
  - 'sshj' and 'bouncycastle' dependencies are added and project syncs.
  - SSH engine can establish a connection and execute a remote command successfully.

### Task_2_Profile_Management_UI: Build the 'SSH Connections' and 'Remote Profiles' management screens using Jetpack Compose and Navigation 3. Implement the UI for adding and editing profiles, including command mapping with placeholders.
- **Status:** COMPLETED
- **Updates:** Implemented type-safe navigation using Jetpack Navigation 3. Created SSH Connections screen with 'Add Connection' dialog and private key picker. Created Remote Profiles screen with 'Add Remote' configuration for mapping commands. Implemented Material 3 Navigation Drawer. Updated AppDatabase to version 2 for RemoteProfile entity. Ensured Edge-to-Edge support.
- **Acceptance Criteria:**
  - Users can create, read, update, and delete SSH and Remote profiles.
  - Navigation between connection list, profile list, and dashboard is functional.
  - The implemented UI must match the design provided in /home/djshaji/AndroidStudioProjects/remotecontroloverssh/input_images/image_0.png.

### Task_3_Remote_Dashboard_UI: Develop the main Interactive Remote Dashboard featuring dual sliders, a 5-way D-pad, and Home/Back buttons. Connect UI events to the SSH command execution engine.
- **Status:** COMPLETED
- **Updates:** Implemented the Remote Interface Screen (Dashboard) with dual sliders, 5-way D-pad, and action buttons. Integrated SSH connection management via SshManager, ensuring persistent sessions. Implemented debounced slider command execution with %val% substitution. Added connection status indicators. UI strictly follows Material 3 and matches the provided design image.
- **Acceptance Criteria:**
  - Dashboard displays dual sliders and a 5-way D-pad as per the design.
  - Moving sliders or pressing buttons triggers the corresponding SSH command execution.
  - The implemented UI must match the design provided in /home/djshaji/AndroidStudioProjects/remotecontroloverssh/input_images/image_0.png.

### Task_4_Final_Polish_Verification: Implement Material 3 theming with a vibrant color scheme, full edge-to-edge display, and an adaptive app icon. Perform a final build and verify application stability and requirement alignment.
- **Status:** COMPLETED
- **Updates:** Refined the D-pad UI to match the circular, large-button design in the mockup. Removed unused placeholder resources. Applied final Material 3 polish and verified build stability. App icon and edge-to-edge support are fully implemented.
- **Acceptance Criteria:**
  - Material 3 theme with vibrant colors and dark/light mode support is applied.
  - Adaptive app icon and edge-to-edge display are implemented.
  - Project builds successfully, app does not crash, and all existing tests pass.
  - The implemented UI must match the design provided in /home/djshaji/AndroidStudioProjects/remotecontroloverssh/input_images/image_0.png.
- **Duration:** N/A

