package mega.privacy.android.domain.entity.camerauploads

/**
 * Data representation of a media retrieved from the media store for camera upload
 *
 * @property filePath File path retrieved from media store
 * @property timestamp Timestamp retrieved from media store.
 *                     It corresponds to the max between the DATE_ADDED and DATE_MODIFIED attributes
 */
data class CameraUploadsMedia(
    val filePath: String,
    val timestamp: Long,
)
