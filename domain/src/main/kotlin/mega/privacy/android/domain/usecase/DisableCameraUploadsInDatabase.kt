package mega.privacy.android.domain.usecase

/**
 * Use Case to disable Camera Uploads by manipulating values in the database
 */
fun interface DisableCameraUploadsInDatabase {

    /**
     * Invocation function
     */
    suspend operator fun invoke()
}