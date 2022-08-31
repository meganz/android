package mega.privacy.android.domain.entity

/**
 * Save retrieved files from media store for camera upload
 */
data class CameraUploadMedia(
    /**
     * File path retrieved from media store
     */
    val filePath: String?,
    /**
     * Time stamp retrieved from media store
     */
    val timestamp: Long,
)
