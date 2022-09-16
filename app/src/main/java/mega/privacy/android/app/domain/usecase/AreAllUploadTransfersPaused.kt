package mega.privacy.android.app.domain.usecase

/**
 * Use case for checking if the queue of pending upload transfers is paused, or if all in progress
 * upload transfers are paused individually.
 */
fun interface AreAllUploadTransfersPaused {

    /**
     * Invokes the use case
     *
     * @return True if all pending upload Transfers are paused, and False if otherwise
     */
    suspend operator fun invoke(): Boolean
}