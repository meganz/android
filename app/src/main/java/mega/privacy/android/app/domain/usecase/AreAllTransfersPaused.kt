package mega.privacy.android.app.domain.usecase

/**
 * Use case for checking if the queue of transfers is paused or if all in progress transfers
 * are paused individually.
 */
fun interface AreAllTransfersPaused {
    /**
     * Invoke.
     *
     * @return True if all the transfers are paused, false otherwise.
     */
    suspend operator fun invoke(): Boolean
}
