package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.usecase.ClearPsa

/**
 * Use case for logging out of the MEGA account without invalidating the session.
 */
fun interface LocalLogout {

    /**
     * Invoke.
     *
     * @param disableChatApi Temporary param for disabling megaChatApi.
     * @param clearPsa       Temporary param for clearing Psa.
     */
    suspend operator fun invoke(disableChatApi: DisableChatApi, clearPsa: ClearPsa)
}