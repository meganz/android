# Tech Spec — Continuous Document Scanner

**Ticket:** AND-23706
**Module:** `feature/document-scanner/document-scanner`
**Companion PRD:** [PRD.md](./PRD.md)
**Last updated:** 2026-05-26

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
- `cachedModelFile()` is a cheap, non-blocking check for the analysis thread; `ensureModelReady()` is the suspending download. It is driven by a `WorkManager` worker (so the fetch survives process death and respects battery / network constraints) and observed by a dedicated prepare screen that shows progress and routes the user to the legacy scanner if the download fails — both land in follow-up MRs.
- The current download URL is a **temporary staging link**; before rollout it moves to the production endpoint, ideally via remote config so the model can be swapped without an app update.

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

## MR plan (this branch → develop)

All MRs use ticket **AND-23706**.

| # | Branch | Scope |
|---|---|---|
| 0 | `juh/and-23706-docs` | PRD + techspec under `feature/document-scanner/document-scanner/docs/`. Lands first so reviewers can refer to the agreed-upon spec as the code MRs come in. |
| 1 | `juh/and-23706-tflite-detector` | TFLite detector, stability, smoother, DI, tflite asset, gradle deps |
| 2 | `juh/and-23706-bitmap-pipeline` | Warper, splitter, hasher, OCR, storage, `CaptureFrameUseCase` |
| 3 | `juh/and-23706-scan-session-vm` | `ScanSessionViewModel` + state (no dedup yet) |
| 4a | `juh/and-23706-camera-screen` | `ContinuousScanScreen` (CameraX, 4K analysis, capture, routing, feature flag) |
| 4b | `juh/and-23706-scan-ui-polish` | Top bar, control bar, status column, page badge, fly-to-thumb |
| 5a | `juh/and-23706-live-dedup` | Boundary-region dHash + motion gate (no banner yet) |
| 5b | `juh/and-23706-dedup-sticky-flip` | Sticky window, page-flip detection, DuplicateBanner |
| 5c | `juh/and-23706-ocr-dedup` | Post-capture OCR dedup + self-improving hash memory |
| 6 | `juh/and-23706-preview-gallery` | Preview dialog + delete + thumb-strip reorder + pause on preview |

## Open items

- Restore parked tests via Robolectric (separate ticket).
- Implement a real `KotlinColumnPageSplitter` for book-spread spine splitting (deferred).
- Reassess OpenCV dependency removal once all MRs land — currently no consumers in the merged scope.
