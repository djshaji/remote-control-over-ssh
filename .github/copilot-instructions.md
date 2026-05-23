# Copilot Instructions

## Build, test, and lint commands

Run commands from the repository root.

- Build the debug app: `./gradlew :app:assembleDebug`
- Run all JVM unit tests: `./gradlew :app:testDebugUnitTest`
- Run one JVM test class: `./gradlew :app:testDebugUnitTest --tests "org.acoustixaudio.opiqo.remotecontroloverssh.ExampleUnitTest"`
- Run one JVM test method: `./gradlew :app:testDebugUnitTest --tests "org.acoustixaudio.opiqo.remotecontroloverssh.ExampleUnitTest.addition_isCorrect"`
- Run Android lint for the debug variant: `./gradlew :app:lintDebug`
- Run instrumentation tests on a connected device/emulator: `./gradlew :app:connectedDebugAndroidTest`
- Run one instrumentation test class: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.acoustixaudio.opiqo.remotecontroloverssh.ExampleInstrumentedTest`
- Run the app module's full build lifecycle: `./gradlew :app:build`

## High-level architecture

- This is a single-module Android app (`:app`) built with Kotlin, Jetpack Compose Material 3, and Navigation 3.
- `MainActivity` hosts `MainApp`, which owns the drawer UI and Navigation 3 back stack. Routes are defined as a serializable sealed class in `ui/Navigation.kt`.
- The app persists all configuration locally in Room through `AppDatabase`. The three core entities are:
  - `SshProfile`: SSH host, port, username, and imported private-key path
  - `RemoteProfile`: a named remote that links to an SSH profile
  - `RemoteCommand`: a per-control command mapping for a remote profile
- `RemoteControlApplication` owns an `AppContainer`, and UI entry points pull an injected `AppRepository` plus per-screen `SshClient` instances from that container. ViewModels no longer open Room directly from `Context`.
- Built-in remote templates are defined in `app/src/main/assets/builtin_remote_profiles.json` and loaded through `BuiltInRemoteProfilesStore`. If you add or rename template commands, keep the JSON keys aligned with `RemoteControlConfig`.
- The app flow is: create SSH profiles in `SshProfilesScreen` -> create remote profiles plus control mappings in `RemoteProfilesScreen` -> open a remote in `DashboardScreen` and execute the mapped commands over SSH.
- `DashboardViewModel` loads the selected remote profile, resolves its linked SSH profile, loads command mappings, then connects through an injected `SshClient` so the dashboard can reuse a persistent SSH session while the screen stays alive.
- `RemoteControlApplication` swaps Android's default Bouncy Castle provider for the full provider before any SSH work, which is required by the `sshj` setup used here.

## Key conventions

- Command bindings are keyed by stable string identifiers such as `DPAD_UP`, `DPAD_DOWN`, `DPAD_LEFT`, `DPAD_RIGHT`, `DPAD_SELECT`, `BTN_BACK`, and `BTN_HOME`. Slider commands are stored per step as keys like `SLIDER_1_STEP_0` through `SLIDER_1_STEP_10` and `SLIDER_2_STEP_0` through `SLIDER_2_STEP_10`.
- `DashboardViewModel` debounces both slider flows by 100 ms before looking up the current step command. Keep that debounce in place for slider-triggered SSH execution.
- Slider controls currently assume an integer range of `0..10`; the UI labels, slider configuration, and stored slider-step keys all follow that expectation.
- SSH private keys are copied from a picked `Uri` into app-internal storage via `KeyFileHelper` and only the internal file path is stored in Room. Do not assume the original `Uri` stays available.
- SSH profiles now require an explicit host fingerprint (`SHA256:...` or `MD5:aa:bb:...`) and a private key before they are considered valid. `SshManager` verifies the server key against that fingerprint instead of using permissive host verification.
- Adding a new remote control is a cross-file change: update the command-entry dialog in `RemoteProfilesScreen`, the save mapping in the repository layer, the button/control UI in `DashboardScreen`, and the execution path in `DashboardViewModel`.
- Room schema changes should be added through explicit migrations in `AppDatabase`, and schema JSON is generated under `app/schemas/`.
