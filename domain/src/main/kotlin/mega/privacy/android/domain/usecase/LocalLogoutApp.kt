package mega.privacy.android.domain.usecase

/**
 * Use case for logging out of the MEGA account without invalidating the session.
 */
fun interface LocalLogoutApp {

    /**
     * Invoke.
     */
    suspend operator fun invoke(clearPsa: ClearPsa)
}