package mega.privacy.android.app.domain.usecase

/**
 * Get the local path for camera upload if exists
 */
interface GetCameraUploadLocalPath {
    /**
     * Invoke
     *
     * @return local path or null
     */
    operator fun invoke(): String?
}
