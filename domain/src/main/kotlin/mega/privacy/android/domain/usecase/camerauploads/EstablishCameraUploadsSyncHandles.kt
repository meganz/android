package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that performs the following functions:
 *
 * 1. Retrieve the Camera Uploads Sync Handles from the API
 * 2. Set the Sync Handles to the local Database
 */
fun interface EstablishCameraUploadsSyncHandles {

    /**
     * Invocation function
     */
    suspend operator fun invoke()
}