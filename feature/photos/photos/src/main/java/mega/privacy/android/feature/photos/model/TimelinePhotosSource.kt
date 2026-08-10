package mega.privacy.android.feature.photos.model

enum class TimelinePhotosSource {
    /**
     * All Cloud drive images + videos in Camera upload and media upload folders
     */
    ALL_PHOTOS,

    /**
     *  Cloud drive without CAMERA_UPLOAD
     */
    CLOUD_DRIVE,

    /**
     * Camera upload and media upload folders
     */
    CAMERA_UPLOAD;

    companion object {
        val DEFAULT = ALL_PHOTOS
    }
}