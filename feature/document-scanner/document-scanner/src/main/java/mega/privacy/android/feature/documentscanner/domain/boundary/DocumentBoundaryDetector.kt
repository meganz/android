package mega.privacy.android.feature.documentscanner.domain.boundary

import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult

/**
 * Detects document boundaries in a camera frame.
 *
 * Implementations are vision-library agnostic; the production detector uses a
 * TFLite UNet. Coordinates in the returned [DetectionResult] must be normalised
 * to the 0-1 range relative to the (post-rotation) frame dimensions.
 */
interface DocumentBoundaryDetector {

    /**
     * Analyse a single grayscale camera frame and return the detected document boundary, if any.
     *
     * @param grayBytes Grayscale pixel data (Y channel)
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @param rotationDegrees Rotation needed to match display orientation (0, 90, 180, 270)
     * @param timestamp Frame timestamp in milliseconds
     * @return A [DetectionResult] if a document was found, or null otherwise
     */
    fun detect(
        grayBytes: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        timestamp: Long,
    ): DetectionResult?

    /**
     * Releases any native resources held by the detector (e.g. a TFLite
     * interpreter and GPU delegate). Safe to call more than once. After release,
     * the next [detect] call re-initialises lazily — this still requires the
     * model file to be on disk (the same precondition as the first [detect]); if
     * it has since been removed, re-initialisation fails.
     *
     * Callers must stop feeding frames before releasing so the analysis thread
     * cannot race the teardown — in this feature the ViewModel releases in
     * `onCleared`, after the screen has unbound the CameraX analyzer.
     */
    fun release()
}
