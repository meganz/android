package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * Result of a single frame boundary detection.
 *
 * Detection confidence lives on [DocumentBoundary.confidence] — there is no
 * separate detection-level score, so callers read `boundary.confidence`.
 *
 * @property boundary The detected document boundary with normalised (0-1) corner coordinates
 * @property frameTimestamp Timestamp of the analysed frame in milliseconds
 * @property frameWidth Width of the analysed frame after rotation (pixels)
 * @property frameHeight Height of the analysed frame after rotation (pixels)
 */
data class DetectionResult(
    val boundary: DocumentBoundary,
    val frameTimestamp: Long,
    val frameWidth: Int,
    val frameHeight: Int,
)
