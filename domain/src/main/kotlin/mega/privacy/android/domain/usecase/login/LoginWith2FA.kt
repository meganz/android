package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.LoginStatus

/**
 * Use case for log in an account with 2FA enabled.
 */
fun interface LoginWith2FA {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param password Account password.
     * @param pin2FA 2FA code.
     * @param disableChatApi [DisableChatApi].
     * @return Flow of [LoginStatus]
     */
    operator fun invoke(
        email: String,
        password: String,
        pin2FA: String,
        disableChatApi: DisableChatApi,
    ): Flow<LoginStatus>
}