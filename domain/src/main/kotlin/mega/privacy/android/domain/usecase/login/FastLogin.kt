package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.login.LoginStatus

/**
 * Use case for fast login.
 */
fun interface FastLogin {

    /**
     * Invoke.
     *
     * @param session Account session.
     * @param refreshChatUrl True if should refresh chat api URL, false otherwise.
     * @param disableChatApi [DisableChatApi]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        session: String,
        refreshChatUrl: Boolean,
        disableChatApi: DisableChatApi,
    ): Flow<LoginStatus>
}