# CLAUDE.md

This file provides guidance to Claude Code when working in the `:feature:document-scanner:document-scanner` module.

> Module path: `:feature:document-scanner:document-scanner` · Build file: `feature/document-scanner/document-scanner/document-scanner.gradle.kts` · Namespace: `mega.privacy.android.feature.documentscanner`

## Overview
Self-contained feature module for the in-app document scanner. It provides a continuous camera-based scanning experience that detects document boundaries in real time using a UNet-based image-segmentation model running on the LiteRT (TFLite) runtime (CPU + GPU delegates).

The model artifact is **not bundled in the APK**; it is downloaded on first use via a WorkManager job (using OkHttp). Before showing the camera, the feature decides between the new scanner and the legacy ML Kit scanner based on a feature flag, network state, and a persisted cellular-data consent preference. Follows the project's Clean Architecture (presentation → domain ← data) within a single module.

## Architecture & Layout
Package root: `mega.privacy.android.feature.documentscanner`
- `presentation/` — `ScanSessionViewModel`, Compose screens (`screen/`) and components (`component/`)
- `domain/` — `entity/`, `repository/` (interfaces), `boundary/` + `smoother/` (abstractions), `launchmode/`, `model/`, `usecase/`
- `data/` — `repository/` (`Default*` implementations), `boundary/` (TFLite detector + geometry helpers), `smoother/`, `model/` (downloading provider), `worker/` (model download)
- `di/` — Hilt `@Module` bindings
- `navigation/` — `FeatureDestination` + Navigation3 entry wiring

## Key Components
- **ViewModels**: `ScanSessionViewModel` (`presentation/`) — currently handles camera-permission state.
- **Use Cases**: `GetScannerLaunchModeUseCase` (`domain/usecase/`) — computes `ScannerLaunchMode` (`New` vs `Legacy(reason)`) from feature flag, network state, and cellular consent; throws `CellularConsentRequiredException` when on cellular without consent.
- **Repositories / Gateways / Data sources**:
  - Domain interfaces: `ScanSessionRepository` (page add/remove/reorder/replace/clear), `ScannerPreferencesRepository` (cellular consent), `DocumentBoundaryDetector`, `StabilityTracker`, `BoundarySmoother`, `ScannerModelProvider`.
  - Data impls: `DefaultScanSessionRepository`, `DefaultScannerPreferencesRepository` (DataStore), `TFLiteBoundaryDetector`, `DefaultStabilityTracker`, `ExponentialMovingAverageBoundarySmoother`, `DownloadingScannerModelProvider`. Helpers: `LargestComponentFinder`, `GrayFrameRotator`, `BoundaryGuard`, `BoundaryGeometry`. Worker: `ScannerModelDownloadWorker`.
- **Navigation**: `ContinuousScanDestination` (implements `FeatureDestination`); routes via `ContinuousScanNavKey` (from `:core:navigation-contract`) using Navigation3 `EntryProviderScope`.
- **UI**: `ContinuousScanScreen` (`presentation/screen/`), `ScannerCloseButton` (`presentation/component/`).

## Module Dependencies
Project modules: `:domain`, `:navigation`, `:core:navigation-contract`, `:resources:icon-pack`, `:resources:string-resources`.
Notable external libs: LiteRT + LiteRT-GPU (on-device segmentation), CameraX (camera2 / lifecycle / view), WorkManager (+ Hilt Work), OkHttp3 (model download), Jetpack DataStore Preferences (cellular consent), Navigation3 runtime, Compose BOM + Material3 + Hilt Navigation, MEGA core-ui (+ tokens), compose-state-events, Timber.

## Testing
JUnit 5 + Mockito + Turbine + Truth, with `androidx.work.test` for WorkManager. Tests cover the ViewModel, repositories, boundary/smoother helpers, and the launch-mode use case.
Run: `./gradlew feature:document-scanner:document-scanner:testDebugUnitTest`

## Notes & Gotchas
- The TFLite model is downloaded at runtime, not packaged. Code must handle the model-not-ready path (`ScannerModelProvider.ensureModelReady`, `ModelDownloadException`) and surface download failures gracefully.
- Respect the cellular-consent flow: `GetScannerLaunchModeUseCase` throws `CellularConsentRequiredException` rather than returning a mode — callers must show the consent prompt before routing.
- This is the new scanner; always preserve the `Legacy` fallback path (ML Kit) keyed off `LegacyReason`.
- See root `.claude/CLAUDE.md` for global architecture, naming, and testing conventions.
