package mega.privacy.android.domain.usecase.login

/**
 * Use case for notifying a logout.
 */
fun interface BroadcastLogout {

    /**
     * Invoke.
     */
    suspend operator fun invoke()
}