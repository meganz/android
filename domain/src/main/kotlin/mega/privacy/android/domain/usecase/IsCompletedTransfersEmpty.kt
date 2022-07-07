package mega.privacy.android.domain.usecase

/**
 * Use case for checking if the completed transfers list is empty.
 */
fun interface IsCompletedTransfersEmpty {

    /**
     * Invoke.
     *
     * @return True if completed transfers is empty, false otherwise.
     */
    suspend operator fun invoke(): Boolean
}