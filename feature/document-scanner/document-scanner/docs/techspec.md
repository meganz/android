# Tech Spec — Continuous Document Scanner

**Epic:** AND-22951 — Multi-page scanning experience (IMDS)
**Backend ticket:** AND-23706 (merged to `develop`)
**Module:** `feature/document-scanner/document-scanner`
**Companion PRD:** [PRD.md](./PRD.md)
**Last updated:** 2026-06-22

> **Status (2026-06):** The TFLite detector, model-download infrastructure, stability/smoother, scan-session repository, and `GetScannerLaunchModeUseCase` are **merged to `develop`**. The presentation layer (download/loading UX, camera screen wiring, capture, multi-page, preview, export) is being **built from scratch** — tracked in [§UI/UX task plan](#uiux-task-plan). Sections below describe the full target design; components not yet on `develop` are called out in the task plan.

## High-level architecture

```
                    ┌─────────────────────────────────────────────┐
                    │           ContinuousScanScreen              │
                    │   (CameraX bind, permissions, layout)       │
                    └────────────┬──────────────┬─────────────────┘
              ImageAnalysis      │              │      ImageCapture
                      ▼          ▼              ▼
            ┌──────────────────────────┐  ┌────────────────────┐
            │  ScanSessionViewModel    │  │    takePicture     │
            │  ─ throttle analysis     │  │  (fallback path)   │
            │  ─ run detector          │  └────────┬───────────┘
            │  ─ stability tracker     │           │
            │  ─ motion gate           │           │ JPEG bytes
            │  ─ live dedup (dHash)    │           ▼
            │  ─ auto-capture trigger  │
            └────────────┬─────────────┘
                         │ CaptureResult
                         ▼
            ┌──────────────────────────────────────────────────────────┐
            │                 CaptureFrameUseCase                       │
            │  decode JPEG → Bitmap                                     │
            │  ├─ PerspectiveWarper  (Bitmap → warped Bitmap)           │
            │  ├─ PageSplitter       (Bitmap → List<Bitmap>)            │
            │  ├─ PageHasher         (Bitmap → Long)        ┐ parallel  │
            │  ├─ PageTextExtractor  (Bitmap → String)      ┘           │
            │  ├─ classifyDuplicate (OCR Jaccard + dHash)               │
            │  └─ DocumentImageStorage.save (Bitmap → file URIs)        │
            └──────────────────────────────────────────────────────────┘
                                  │ ScannedPage
                                  ▼
            ┌──────────────────────────────────────────────────────────┐
            │            ScanSessionRepository (in-memory)              │
            └──────────────────────────────────────────────────────────┘
```

## Module layout

```
feature/document-scanner/document-scanner/
├── src/main/java/.../
│   ├── domain/
│   │   ├── boundary/      DocumentBoundaryDetector, StabilityTracker
│   │   ├── smoother/      BoundarySmoother
│   │   ├── warper/        PerspectiveWarper
│   │   ├── splitter/      PageSplitter
│   │   ├── hasher/        PageHasher
│   │   ├── ocr/           PageTextExtractor
│   │   ├── storage/       DocumentImageStorage, SavedImage
│   │   ├── repository/    ScanSessionRepository
│   │   ├── usecase/       CaptureFrameUseCase, CaptureResult
│   │   └── entity/        DocumentBoundary, ScannedPage, ScanSession, …
│   ├── data/
│   │   ├── boundary/      TFLiteBoundaryDetector, DefaultStabilityTracker
│   │   ├── model/         DownloadingScannerModelProvider
│   │   ├── smoother/      ExponentialMovingAverageBoundarySmoother
│   │   ├── warper/        BitmapPerspectiveWarper
│   │   ├── splitter/      PassthroughPageSplitter
│   │   ├── hasher/        DHashPageHasher
│   │   ├── ocr/           MlKitTextExtractor
│   │   ├── storage/       DefaultDocumentImageStorage, BitmapThumbnailEncoder
│   │   └── repository/    DefaultScanSessionRepository
│   ├── di/                One @Module per pipeline stage
│   ├── presentation/
│   │   ├── ScanSessionViewModel, ScanSessionUiState
│   │   ├── model/         BoundaryOverlayState, PreviewPage
│   │   ├── component/     BoundaryOverlay, CaptureFlyToThumbnail,
│   │   │                   CapturedPagePreviewDialog, DuplicateBanner,
│   │   │                   PageCountBadge, PageThumbStrip, ScanGuideOverlay,
│   │   │                   ScannerControlBar, ScannerTopBar, ScanStatusColumn
│   │   └── screen/        ContinuousScanScreen
│   └── navigation/        ContinuousScanDestination
└── docs/                  PRD.md, techspec.md
```

## Key components

### `TFLiteBoundaryDetector` (data layer)
- Loads a UNet+ResNet-34 model. Input: 512×512 RGB, ImageNet normalised. Output: binary mask.
- The model file is **not bundled in the APK** — it is downloaded on first use and cached by `ScannerModelProvider` (see "Model distribution" below). The detector loads the interpreter from the cached `File` on the first `detect()` call and holds it for the process lifetime. Callers must call `ensureModelReady()` before invoking `detect()` — a present model file is a precondition.
- GPU delegate via `litert-gpu`; falls back to CPU on init failure. Releasing the interpreter on scan-session teardown lands with the ScanSessionViewModel MR (designed alongside the threading model so the analyzer thread cannot race the close).
- Post-process: largest connected component → 4 extreme corners (`LargestComponentFinder`) → guard (`BoundaryGuard`).
- **Guards** (`BoundaryGuard`):
  - `MIN_FILL_RATIO = 0.88` — mask must fill ≥ 88% of the bounding quad (rejects hand-on-page).
  - `MIN_OPPOSITE_SIDE_RATIO = 0.55` — rejects severely trapezoidal detections.
  - Verdict returned as a typed `RejectReason`; the detector logs it at `[DocScanner][guard]`.

### Model distribution
- The ~93 MB `.tflite` is hosted off-app and fetched at runtime, so it never bloats the APK and is never committed to git.
- `DownloadingScannerModelProvider` downloads it (OkHttp) into `filesDir/scanner-models/midv500_unet.tflite`, streaming to a `.tmp` sidecar and renaming into place only after it passes integrity checks: exact **size** (97,867,228 bytes) and **SHA-256** (`e0c37a9a…b4e55`), both hardcoded. A cached file that fails verification is deleted and re-downloaded.
- `cachedModelFile()` is a cheap, non-blocking check for the analysis thread; `ensureModelReady()` is the suspending download, driven by `ScannerModelDownloadWorker` (survives process death) and observed by the prepare screen via `WorkInfo.progress`.
- **Enqueue policy (set by the presentation layer, not the worker — see [§Launch routing](#launch-routing--prepare-screen)):**
  - Wi-Fi, or cellular **with** consent → enqueue with `NetworkType.CONNECTED` and run now; the user waits on the Loading screen.
  - Cellular **without** consent (user declined the metered download) → enqueue with `NetworkType.UNMETERED` so WorkManager defers the fetch until Wi-Fi, and route to legacy meanwhile.
  - Both add `setRequiresBatteryNotLow(true)`. Enqueue as `enqueueUniqueWork(UNIQUE_NAME, KEEP, …)` so tapping "Scan" repeatedly doesn't stack downloads.
- The current download URL is a **temporary staging link**; before rollout it moves to the production endpoint, ideally via remote config so the model can be swapped without an app update.

### Launch routing & prepare screen
`GetScannerLaunchModeUseCase` (merged) decides flag/offline/cellular-consent up front and throws `CellularConsentRequiredException` on metered-without-consent. The presentation layer wraps it with the cache check and the download UX:

1. Resolve `ScannerLaunchMode`. `Legacy(reason)` → open ML Kit. `New` → step 2.
2. `cachedModelFile() != null` → navigate straight to the camera screen.
3. Not cached → show the **download-confirmation dialog** (Wi-Fi and cellular variants), then either enqueue per the policy above and navigate to the **prepare screen**, or route to legacy.
4. On `CellularConsentRequiredException`, the dialog's cellular variant decides: consent → persist + immediate download; decline → `UNMETERED` background enqueue + legacy.

> **Routing model decision (UI/UX rebuild):** because the confirmation dialog must show even on Wi-Fi when the model isn't cached, "needs download" is promoted to an explicit routing outcome rather than being folded into `New`. Implement as either an added `ScannerLaunchMode.NeedsDownload` arm or a presentation-side cache gate ahead of `New`; the task plan picks one in the routing ticket.

**Prepare screen** (`PrepareScannerScreen` + VM, new): observes `WorkInfo` for `UNIQUE_NAME`. Renders progress from `WorkInfo.progress` (`KEY_BYTES_DOWNLOADED` / `KEY_TOTAL_BYTES`). `SUCCEEDED` → auto-navigate to the camera screen (unless the user already left for legacy). `FAILED` → read `KEY_FAILURE_REASON` (`permanent` vs `transient`), show the matching message, and offer legacy + retry. A **"Use old scanner"** button routes to legacy **without cancelling the work**, so the download completes in the background.

### `DefaultStabilityTracker`
- Per-frame Euclidean corner drift in normalised coords.
- `DRIFT_THRESHOLD = 0.05f` (≈ 5% of frame width).
- `STABLE_FRAMES = 2` ≈ 400 ms at the 200 ms analysis throttle.

### `ScanSessionViewModel` — analysis pipeline
Per analysis frame:
1. Throttle to `ANALYSIS_INTERVAL_MS = 200`.
2. Y-plane → grayscale bytes for the detector.
3. `DocumentBoundaryDetector.detect()` → optional `DetectionResult`.
4. `BoundarySmoother.smooth()` (EMA on the corners).
5. `StabilityTracker.onDetectionResult()` → `SEARCHING` / `UNSTABLE` / `STABILIZING` / `STABLE`.
6. Compute boundary-region 9×8 dHash inside the quad (bilinear interp).
7. Motion gate: previous-vs-current hash Hamming > `MOTION_HAMMING_THRESHOLD = 50` → demote to UNSTABLE.
8. Live dedup: hash within `LIVE_DUPLICATE_HAMMING_THRESHOLD = 14` of any prior captured-frame hash → mark duplicate (sticky 2 s).
9. Update UI state. If effective state is STABLE and no other gates trip, fire `autoCaptureTrigger`.

### `CaptureFrameUseCase` — per-capture pipeline
Decode once, run the rest in-memory:
```kotlin
val src = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
val warped = perspectiveWarper.warp(src, boundary, rotationDegrees)
val pages  = pageSplitter.split(warped)
for (page in pages) {
    val hash = pageHasher.hash(page)
    val text = pageTextExtractor.extractText(page)   // ML Kit Latin, downscaled to 1500 px
    if (classifyDuplicate(hash, text, knownPages) == DUPLICATE) {
        duplicateCount++; continue
    }
    val saved = documentImageStorage.save(page, sessionId)
    addPage(ScannedPage(…, contentHash = hash, extractedText = text))
}
```
`classifyDuplicate`:
- Per existing page: compute OCR Jaccard on word sets, dHash Hamming on warped Bitmap.
- DUPLICATE if `jaccard ≥ TEXT_SIMILARITY_THRESHOLD = 0.5` **or** `hammingDist ≤ DUPLICATE_HAMMING_THRESHOLD = 8`.

### Capture path selection
```kotlin
if (uiState.analysisFrameIsHighRes) onCaptureFromAnalysisFrame()
else                                performCapture()           // ImageCapture.takePicture
```
`analysisFrameIsHighRes` is set when `ImageAnalysis.resolutionInfo.resolution.maxEdge ≥ ANALYSIS_FRAME_HD_THRESHOLD_PX = 2400`. The analysis stream cached most-recent NV21 bytes are converted to JPEG via `YuvImage.compressToJpeg` for the analysis-frame path.

### Preview screen
- `Dialog(decorFitsSystemWindows = false)` so the themed `pageBackground` paints behind the system bars.
- `HorizontalPager` for swipe between pages; tap the top-bar trash for delete with confirmation.
- `PageThumbStrip` (`LazyRow` of small thumbnails). Tap → jump pager. Long-press → drag-to-reorder; non-dragged thumbs animate one slot aside to show the drop target.
- `rememberUpdatedState` on the pages list inside the drag detector so reordering doesn't leave the closure with a stale captured index.
- `ContinuousScanScreen` re-keys its CameraX `DisposableEffect` on `isPaused`; opening the preview unbinds the sensor + analyser + capture use case until the preview closes.

## Data flow — single page capture

```
camera frame                      ┌────────┐
   ▼                              │   VM   │
analyseFrame(ImageProxy) ────────►│        │
                                  │ detect │
                                  │ stable │  STABLE ─► autoCaptureTrigger
                                  └───┬────┘
                                      │
                       capture (analysis frame OR takePicture)
                                      ▼
                               JPEG bytes + rotation
                                      ▼
                           CaptureFrameUseCase
                                      ▼
                             ScanSessionRepository.addPage
                                      ▼
                            uiState.pageCount++, latestThumbnailUri
                                      ▼
                          UI: shutter flash, fly-to-thumb animation
```

## Performance notes

- **Decode once.** All five pipeline stages take `Bitmap`. No JPEG re-encode round-trips between stages. JPEG re-encode happens exactly twice per page: full-resolution save (quality 92) and thumbnail (max edge 512, quality 80).
- **OCR downscale.** ML Kit is fed a Bitmap with `MAX_OCR_EDGE_PX = 1500`, halving processing time on 4K analysis frames without measurable quality loss for document text.
- **GPU delegate.** Boundary detection on the Adreno GPU runs at ~20-25 ms vs ~80 ms on CPU on a Pixel 6.
- **Analysis throttle.** `ANALYSIS_INTERVAL_MS = 200` (5 fps) — enough for stability detection without burning CPU.
- **Pause on preview.** Unbinds the entire CameraX use case stack while the preview screen is open.

## Dedup tuning constants

| Constant | Value | Purpose |
|---|---|---|
| `LIVE_DUPLICATE_HAMMING_THRESHOLD` | 14 | Live boundary-region dHash match → duplicate. |
| `DUPLICATE_HAMMING_THRESHOLD` | 8 | Post-warp dHash match → duplicate. |
| `TEXT_SIMILARITY_THRESHOLD` | 0.5 | OCR Jaccard match → duplicate. |
| `DUPLICATE_STICKY_MS` | 2_000 | How long the banner stays after a match. |
| `DUPLICATE_SUPPRESS_AFTER_CAPTURE_MS` | 1_500 | Force-hide banner immediately after we captured a page. |
| `PAGE_FLIP_CLEAR_FRAMES` | 3 | Consecutive non-matching frames to clear awaitingPageFlip. |
| `AUTO_CAPTURE_COOLDOWN_MS` | 3_000 | Hard cooldown after any capture. |

## Testing

- **Unit:** stability tracker, boundary smoother, repository, mapper logic — all JVM, no Android dependencies.
- **Parked:** `CaptureFrameUseCaseTest`, `DefaultDocumentImageStorageTest`, two stability dwell tests — disabled via `@Disabled` with KDoc explaining the Robolectric / mockable-decoder need. Restore is a separate ticket.
- **Manual:** smoke test on GMS QA build per MR.

## Rollout plan

1. Land MRs in the order in [PRD §Rollout](./PRD.md#rollout) under `AND-23706`. Each MR builds on the previous one and ships dark behind the existing feature flag.
2. Internal QA → staff dogfood → 1% / 10% / 100% prod ramp.
3. Remove the legacy ML Kit scanner code path once 100% has been stable for two release cycles.

## UI/UX task plan

Backend is merged under AND-23706; this plan covers the **presentation layer rebuild** as IMDS tickets under epic **AND-22951**. Each row is one Jira ticket / branch and ships dark behind the `ContinuousDocumentScanner` flag.

| # | Ticket | Scope | Depends on |
|---|---|---|---|
| **Phase 1 — First-run download & launch UX** | | | |
| U1 | AND-23983 | Launch routing: promote "needs download" to an explicit outcome; entry-point checks cache + routes to dialog/prepare/legacy. Wraps `GetScannerLaunchModeUseCase`. | backend |
| U2 | AND-23984 | Download-confirmation dialog (Wi-Fi + cellular variants) + persist cellular consent. | U1 |
| U3 | AND-23985 | Download enqueue policy: Wi-Fi/consent → `CONNECTED` now; declined-cellular → `UNMETERED` background. `enqueueUniqueWork(KEEP)`. | U2 |
| U4 | AND-23986 | `PrepareScannerScreen` + VM: `WorkInfo` progress, "use old scanner" (background-continue), failure→legacy+retry, success→auto-enter camera. | U3 |
| **Phase 2 — Scan camera UX** | | | |
| U5 | _new_ | Wire `ImageAnalysis` → `TFLiteBoundaryDetector` in `ContinuousScanScreen`; `BoundaryOverlay` + `ScanGuideOverlay`; richer `ScanSessionViewModel` (detect/stability state). Built fresh against the merged TFLite contracts — no OpenCV. | U4 |
| U6 | _new_ | Scanner chrome: top bar, manual shutter, **"switch back to old scanner"** control. | U5 |
| U7 | _new_ | Auto-capture-on-stable trigger + capture feedback (flash, fly-to-thumbnail). | U6 |
| **Phase 3 — Capture pipeline & pages** | | | |
| U8 | _new_ | Per-capture pipeline: perspective warp + JPEG save + `ScannedPage` → session repo (`CaptureFrameUseCase`, storage, warper — not yet on develop). | U7 |
| U9 | _new_ | Multi-page UI: page-count badge + thumb strip. | U8 |
| U10 | _new_ | Preview gallery: view / delete / reorder; unbind CameraX while open. | U9 |
| **Phase 4 — Export** | | | |
| U11 | _new_ | Export captured pages to PDF / images + upload to Cloud Drive. | U10 |

**Deferred (not v1):** two-layer dedup (live dHash + OCR Jaccard, PRD §F5), spine-splitting, restoring parked Robolectric tests. Track as follow-ups once the core flow ships.

## Open items

- Restore parked tests via Robolectric (separate ticket).
- Implement a real `KotlinColumnPageSplitter` for book-spread spine splitting (deferred).
- OpenCV is abandoned: the detector is TFLite-only and the merged scope contains no OpenCV code or dependency.
