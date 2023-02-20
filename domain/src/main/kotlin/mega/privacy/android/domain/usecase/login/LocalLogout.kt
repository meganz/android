package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.usecase.ClearPsa

/**
 * Use case for logging out of the MEGA account without invalidating the session.
 */
fun interface LocalLogout {

    /**
     * Invoke.
     */
    suspend operator fun invoke(clearPsa: ClearPsa)
}