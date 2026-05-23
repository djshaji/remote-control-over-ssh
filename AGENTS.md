# AGENTS.md

## Project Snapshot
- Single-module Android app (`:app`) using Kotlin + Compose Material 3 + Navigation 3.
- Entry point is `app/src/main/java/org/acoustixaudio/opiqo/remotecontroloverssh/MainActivity.kt`; app-wide wiring lives in `RemoteControlApplication` + `AppContainer`.
- Core purpose: map UI controls to shell commands and execute them over a persistent SSH session.

## Architecture That Matters
- UI/navigation: `MainApp()` owns drawer + `NavDisplay`; routes are typed in `ui/Navigation.kt` (`NavRoute` sealed class).
- Dependency boundary: screens construct ViewModels with injected `AppRepository` and per-screen `SshClient` from `AppContainer`; avoid direct Room access from UI/ViewModels.
- Data layer: Room DB in `data/AppDatabase.kt` with entities `SshProfile`, `RemoteProfile`, `RemoteCommand` and DAOs in `data/*Dao.kt`.
- Repository behavior: `RoomAppRepository.saveRemoteProfile()` rewrites command mappings transactionally (delete-all then insert non-blank) in `data/AppRepository.kt`.
- SSH boundary: `ssh/SshClient.kt` interface; `ssh/SshManager.kt` is concrete implementation used in production.

## End-to-End Runtime Flow
- User creates SSH profiles in `ui/profiles/SshProfilesScreen.kt`.
- User creates remote profiles + command mappings in `ui/profiles/RemoteProfilesScreen.kt`.
- Dashboard opens via `NavRoute.Dashboard(remoteProfileId)` and `DashboardViewModel` loads repository data, connects SSH, and executes mapped commands.
- Built-in templates come from `app/src/main/assets/builtin_remote_profiles.json` through `AssetBuiltInRemoteProfilesStore`.

## Project-Specific Conventions (Do Not Break)
- Control IDs are stable string keys in `data/RemoteControlConfig.kt` (`DPAD_UP`, `BTN_HOME`, etc.).
- Slider keys are step-specific: `SLIDER_1_STEP_0..10` and `SLIDER_2_STEP_0..10`; slider range is hardcoded `0..10` in UI + config.
- `DashboardViewModel` debounces both sliders by `100ms` before command execution; behavior is covered by `DashboardViewModelTest`.
- Legacy slider fallback still exists: if per-step key missing, `resolveSliderCommand()` can use `SLIDER_1`/`SLIDER_2` command with `%val%` replacement.
- SSH profiles require private key + host fingerprint; validation logic is in `ui/profiles/ProfileValidation.kt` and verification is enforced in `SshManager.connect()`.
- Private keys are copied into app-internal storage via `util/KeyFileHelper.kt`; persisted `Uri` is not relied on.

## Build/Test/Lint Commands (Repo Root)
- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:testDebugUnitTest --tests "org.acoustixaudio.opiqo.remotecontroloverssh.ui.dashboard.DashboardViewModelTest"`
- `./gradlew :app:lintDebug`
- `./gradlew :app:connectedDebugAndroidTest`
- `./gradlew :app:build`

## High-Value Change Workflows
- Adding a new remote control is cross-file: update command inputs in `RemoteProfilesScreen`, key constants/resolution in `RemoteControlConfig`, UI trigger in `DashboardScreen`, and execution path in `DashboardViewModel`.
- If changing built-in template keys, keep `builtin_remote_profiles.json` aligned with `RemoteControlConfig` constants.
- Room schema changes require explicit migration in `AppDatabase` and updated exported schema under `app/schemas/` (Room KSP arg already set in `app/build.gradle.kts`).
- SSH stack assumptions: `RemoteControlApplication` replaces Android BC provider before SSH usage; do not remove this without re-validating `sshj` behavior.

## Testing Anchors
- Unit tests: slider resolution (`data/RemoteControlConfigTest.kt`), validation rules (`ui/profiles/ProfileValidationTest.kt`), slider debounce/command execution (`ui/dashboard/DashboardViewModelTest.kt`).
- Instrumented DB behavior: FK cascade/set-null semantics in `androidTest/.../AppDatabaseInstrumentedTest.kt`.

