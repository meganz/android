package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.LoginStatus

/**
 * Use case for logging.
 */
fun interface Login {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param password Account password.
     * @param disableChatApi [DisableChatApi]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        email: String,
        password: String,
        disableChatApi: DisableChatApi,
    ): Flow<LoginStatus>
}