# PRD — Continuous Document Scanner

**Ticket:** AND-23706 — Implement the scan feature by using TFLite
**Owner:** Juh
**Status:** In implementation (branch `juh/imdsp10-spine-splitting`)
**Last updated:** 2026-05-26

## Background

MEGA's existing in-app scanner relies on the ML Kit document scanner module, which is a black-box UI and a one-shot capture flow: the user has to tap the shutter for every page. Power users scanning multi-page documents (contracts, receipts, books) report this as the single biggest friction. We replaced the legacy scanner module in `CC-8530` but the IMDSP series is meant to ship a first-party, continuous-scan replacement that we own end-to-end.

## Goals

1. **Continuous capture** — point the camera at a page, the app finds the document, the shutter fires automatically when the doc is held steady. The user can immediately move to the next page; no taps required between pages.
2. **High-quality crops** — perspective-corrected, JPEG-encoded scans at print-suitable resolution, ready to be assembled into a PDF.
3. **No duplicates** — if the user accidentally re-captures the same page, the second capture is silently dropped and the UX tells them the page was already scanned.
4. **First-party** — no dependency on the ML Kit scanner module. We control the model, the camera pipeline, the UX.

## Non-goals (for this ticket)

- PDF export and OCR'd PDF text layer (separate ticket).
- Cloud-side dedup or sharing.
- Multi-document spine-splitting (single page per capture for v1; the splitter interface is in place but uses a passthrough impl).
- Tablet / landscape-optimised layouts.

## User stories

| # | As a... | I want to... | so that... |
|---|---|---|---|
| 1 | sales rep | hold my phone over a stack of receipts and have each page captured as I flip through them | I don't have to babysit the shutter |
| 2 | student | scan a textbook chapter quickly | I can revise on the train |
| 3 | accountant | re-scan a page if the crop was bad | I'm not stuck with a bad capture |
| 4 | anyone | trust that the scanner won't capture the same page twice | I don't end up with a 20-page PDF for a 10-page document |
| 5 | anyone | see what I've already captured and remove or reorder pages | I can fix mistakes before exporting |

## Functional requirements

### F1. Boundary detection
- A TFLite UNet model (ResNet-34 encoder) runs on every analysis frame.
- The detected quadrilateral is drawn as a translucent blue overlay with a solid border on top of the camera preview.
- Detection runs on the GPU delegate when available, CPU fallback otherwise.
- The ~93 MB model is **downloaded on first use**, not bundled in the APK (keeps install size small). It is cached on device and integrity-checked (size + SHA-256). First scanner launch shows a one-time "preparing" state while it downloads; subsequent launches are instant.

### F2. Auto-capture
- When the user holds the camera steady on a detected document for ~400 ms, the shutter fires automatically (manual capture also available via the shutter button).
- A motion gate (boundary-region hash) prevents firing on jittery or moving hands.
- An "AUTO" toggle in the top bar lets the user disable auto-capture entirely.

### F3. Capture-path selection
- The pipeline prefers the in-stream `ImageAnalysis` frame when it resolves at ≥ ~2K on the long edge (gives the same DPI as `takePicture` with no shutter latency).
- Falls back to `ImageCapture.takePicture` when the device's analysis stream caps below that threshold.

### F4. Per-page processing
For every captured frame the pipeline runs (in order):
1. Decode JPEG → `Bitmap` (once).
2. Perspective warp using the detected quad.
3. Page splitter (currently passthrough; spine-splitting reserved for follow-up).
4. dHash + ML Kit OCR (parallel).
5. Duplicate classification — drop if duplicate, else save.
6. JPEG re-encode + thumbnail.

### F5. Duplicate detection (two-layer)
- **Layer 1 — live, pre-shutter:** boundary-region 9×8 dHash compared against every previously captured frame's hash. Sticky 2 s window absorbs flicker. Marks the overlay green + shows the "Page already scanned" banner, blocks auto-capture.
- **Layer 2 — post-capture:** OCR text Jaccard similarity (`≥ 0.5` = duplicate) + warped-bitmap dHash (`≤ 8` Hamming = duplicate). If either trips, the page is dropped and we surface the same banner the live path uses.
- Hashes from OCR-rejected captures are added to Layer 1's memory so the next attempt is caught pre-shutter (self-improving).

### F6. Preview gallery
- Tap the page-count badge to enter a full-screen, themed preview of all captured pages.
- Swipe between pages.
- Trash icon → delete (with confirmation).
- Long-press a thumbnail in the bottom strip → drag to reorder. Other thumbs slide aside to show the drop slot.
- CameraX is unbound while the preview is open (battery / heat).

### F7. UX feedback
- Filled translucent blue boundary quad (green when the detected page is already scanned).
- "Capturing..." status indicator while a capture is in flight.
- Fly-to-thumbnail animation: a small thumbnail of the captured page lifts off the detection quad and glides into the bottom-left deck.
- "FLIP TO NEXT PAGE" prompt while we're waiting for the user to move off the just-captured page.

## Success metrics

- ≥ 80% of multi-page sessions complete with zero manual shutter taps (auto-capture mode).
- < 5% duplicate-capture rate (duplicate / total captures) after dedup.
- Time-from-stable to capture committed: < 1 s on mid-tier devices.
- Crash-free rate on the scanner ≥ 99.9% over the first release window.

## Risks & open questions

| Risk | Mitigation |
|---|---|
| TFLite model doesn't generalise to non-Latin scripts | Validate against multilingual document set before release; fall back to motion-only stability if confidence is low. |
| 4K `ImageAnalysis` not supported on enough devices | Fallback chain to `takePicture` already implemented. |
| ML Kit OCR latency on long pages | OCR runs post-shutter, off the hot path; downscale to 1500 px max edge before OCR. |
| Power consumption from continuous inference | GPU delegate; analysis throttled to 200 ms; CameraX unbound when preview is open. |
| First-use model download (~93 MB) fails or is slow on poor networks | Integrity-checked download with atomic cache; show clear "preparing"/retry UX; model fetched once then cached. Consider Wi-Fi-preferred / background prefetch later. |

## Rollout

- Behind the existing `ContinuousDocumentScanner` feature flag (`AND-23073`).
- Internal QA → staff dogfood → 1% prod ramp → 10% → 100%.
- Old ML Kit scanner remains the default until the flag is fully ramped.
