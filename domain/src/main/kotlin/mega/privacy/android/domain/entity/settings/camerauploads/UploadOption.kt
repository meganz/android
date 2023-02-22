package mega.privacy.android.domain.entity.settings.camerauploads

/**
 * Enum class for Camera Uploads that defines what content should be uploaded in Camera Uploads
 */
enum class UploadOption {

    /**
     * Camera Uploads will only upload Photos
     */
    PHOTOS,

    /**
     * Camera Uploads will only upload Videos
     */
    VIDEOS,

    /**
     * Camera Uploads will upload both Photos and Videos
     */
    PHOTOS_AND_VIDEOS,
}