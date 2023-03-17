package mega.privacy.android.domain.usecase.login

/**
 * Use case for notifying an account update.
 */
fun interface BroadcastAccountUpdate {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}