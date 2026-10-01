package mega.privacy.android.domain.entity.settings.camerauploads

/**
 * Enum class for Camera Uploads that defines what content should be uploaded in Camera Uploads
 *
 * @property position The position in the list of Upload Options
 */
enum class UploadOption(val position: Int) {

    /**
     * Camera Uploads will only upload Photos
     */
    PHOTOS(0),

    /**
     * Camera Uploads will only upload Videos
     */
    VIDEOS(1),

    /**
     * Camera Uploads will upload both Photos and Videos
     */
    PHOTOS_AND_VIDEOS(2),
}