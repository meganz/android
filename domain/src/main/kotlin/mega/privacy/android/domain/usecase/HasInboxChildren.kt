package mega.privacy.android.domain.usecase

/**
 * Use case for checking if the Inbox node has children.
 */
fun interface HasInboxChildren {

    /**
     * Invoke.
     *
     * @return True if Inbox has children, false otherwise.
     */
    suspend operator fun invoke(): Boolean
}