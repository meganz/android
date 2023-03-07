package mega.privacy.android.domain.usecase

/**
 * Use case to delete camera upload temporary root directory
 */
fun interface DeleteCameraUploadTemporaryRootDirectory {
    /**
     * Invoke method
     * @return true if operation successful otherwise false
     */
    suspend operator fun invoke(): Boolean
}
