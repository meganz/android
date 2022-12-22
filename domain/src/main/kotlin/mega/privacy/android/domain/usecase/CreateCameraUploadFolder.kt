package mega.privacy.android.domain.usecase

/**
 * Create CameraUpload Folder "Camera Uploads" or "Media Uploads"
 */
fun interface CreateCameraUploadFolder {

    /**
     * invoke
     * @param name
     */
    suspend operator fun invoke(name: String): Long?
}
