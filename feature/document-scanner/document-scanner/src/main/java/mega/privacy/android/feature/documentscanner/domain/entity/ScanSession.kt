package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * Represents a document scanning session containing multiple pages.
 *
 * @property id Unique identifier for this session
 * @property pages List of scanned pages in order
 * @property captureMode The capture mode used during this session
 * @property createdAt Timestamp when the session was started
 */
data class ScanSession(
    val id: String,
    val pages: List<ScannedPage>,
    val captureMode: CaptureMode,
    val createdAt: Long,
)
