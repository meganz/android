package mega.privacy.android.feature.documentscanner.domain.capture

import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage

/**
 * Turns a captured camera frame into a persisted [ScannedPage].
 *
 * The signature is deliberately free of Android graphics types so this contract
 * stays in the domain layer, mirroring [mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector]:
 * the implementation decodes the JPEG, rectifies it to [boundary], writes the
 * full-resolution image and a thumbnail to storage, and returns the resulting
 * page. All bitmap work stays behind this boundary in the data layer.
 *
 * @param jpegBytes The captured frame as JPEG-encoded bytes.
 * @param rotationDegrees Clockwise rotation needed to make the frame upright.
 * @param boundary The detected document quad (normalised 0–1), or null for a
 *   manual capture with no detection — in which case the full frame is kept.
 */
interface DocumentPageCapturer {
    suspend fun capture(
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        boundary: DocumentBoundary?,
    ): ScannedPage
}
