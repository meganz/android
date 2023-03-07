package mega.privacy.android.domain.usecase

/**
 * Create camera upload temporary root directory on a specified path
 */
fun interface CreateCameraUploadTemporaryRootDirectory {

    /**
     * invoke
     * @return created directory path [String]
     */
    suspend operator fun invoke(): String
}
