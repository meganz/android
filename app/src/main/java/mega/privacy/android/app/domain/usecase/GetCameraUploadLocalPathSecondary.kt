package mega.privacy.android.app.domain.usecase

/**
 * Get the secondary local path for camera upload if exists
 */
interface GetCameraUploadLocalPathSecondary {
    /**
     * Invoke
     *
     * @return secondary local path or null
     */
    suspend operator fun invoke(): String?
}
