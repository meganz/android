package mega.privacy.android.domain.usecase

/**
 * Broadcast Camera Upload Pause State
 */
fun interface BroadcastCameraUploadProgress {
    /**
     * Invoke
     *
     * @param progress value representing progress between 0 and 100;
     * @param pending value representing pending elements waiting for upload
     */
    suspend operator fun invoke(progress: Int, pending: Int)
}
