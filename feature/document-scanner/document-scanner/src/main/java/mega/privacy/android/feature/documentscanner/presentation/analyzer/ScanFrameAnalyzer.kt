package mega.privacy.android.feature.documentscanner.presentation.analyzer

import mega.privacy.android.feature.documentscanner.data.boundary.YPlaneToGrayConverter

/**
 * Orchestrates the per-frame preprocessing that sits between CameraX's
 * `ImageAnalysis` callback and the ViewModel's detection loop: throttle to the
 * analysis cadence, then convert the Y-plane to a dense grayscale frame.
 *
 * Extracted from the camera screen so this glue is unit-testable without an
 * `ImageProxy`. The screen supplies the plane bytes lazily via [analyze]'s
 * `planeBytes` provider so the (potentially multi-MB) copy is only paid on
 * frames that actually pass the throttle.
 *
 * Not thread-safe: driven from CameraX's single `ImageAnalysis` executor.
 *
 * @param intervalMs Minimum time between processed frames (analysis cadence).
 */
internal class ScanFrameAnalyzer(
    intervalMs: Long,
    private val grayConverter: YPlaneToGrayConverter = YPlaneToGrayConverter(),
) {
    private val rateLimiter = FrameRateLimiter(intervalMs)

    /**
     * A dense grayscale frame ready for the boundary detector.
     *
     * Not intended for structural equality: as a data class holding a
     * [ByteArray], the generated `equals`/`hashCode` compare [bytes] by
     * reference, not by content. It is only ever used as a transient carrier.
     */
    data class GrayFrame(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        val rotationDegrees: Int,
        val timestampMs: Long,
    )

    /**
     * Returns a [GrayFrame] when [timestampMs] passes the throttle, or null when
     * the frame should be skipped. [planeBytes] is invoked only when the frame
     * is processed, so callers can defer copying the Y-plane buffer.
     */
    fun analyze(
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
        timestampMs: Long,
        planeBytes: () -> ByteArray,
    ): GrayFrame? {
        if (!rateLimiter.shouldProcess(timestampMs)) return null
        val gray = grayConverter.convert(planeBytes(), width, height, rowStride, pixelStride)
        return GrayFrame(gray, width, height, rotationDegrees, timestampMs)
    }
}
