package mega.privacy.android.feature.photos.model

enum class FilterMediaSource {
    /**
     * All Cloud drive images + videos in Camera upload and media upload folders
     */
    AllPhotos,

    /**
     *  Cloud drive without CAMERA_UPLOAD
     */
    CloudDrive,

    /**
     * Camera upload and media upload folders
     */
    CameraUpload
}
