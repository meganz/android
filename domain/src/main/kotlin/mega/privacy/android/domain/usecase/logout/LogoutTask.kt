package mega.privacy.android.domain.usecase.logout

/**
 * Logout task
 *
 * Any logic that needs to be performed on logout should implement this interface and
 * injected into the set of similar tasks.
 */
fun interface LogoutTask {
    suspend operator fun invoke()
}