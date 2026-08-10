package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * A single scanned page within a scan session.
 *
 * @property id Unique identifier for this page
 * @property imageUri URI string to the full-resolution processed image
 * @property thumbnailUri URI string to a downscaled thumbnail
 * @property order Position of this page in the document (0-based)
 * @property capturedAt Timestamp when the page was captured
 * @property quality Quality assessment result
 * @property boundary Detected document boundary, null if captured manually without detection
 */
data class ScannedPage(
    val id: String,
    val imageUri: String,
    val thumbnailUri: String,
    val order: Int,
    val capturedAt: Long,
    val quality: PageQuality,
    val boundary: DocumentBoundary?,
)
