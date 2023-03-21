package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case to retrieve the Camera Uploads Sync Handles from the API
 */
fun interface GetCameraUploadsSyncHandles {
    /**
     * Invocation function
     *
     * @return A potentially nullable [Pair] of Camera Uploads Sync Handles
     *
     * [Pair.first] represents the Primary Folder Sync Handle for the Camera Uploads folder
     * [Pair.second] represents the Secondary Folder Sync Handle for the Media Uploads folder
     */
    suspend operator fun invoke(): Pair<Long, Long>?
}
