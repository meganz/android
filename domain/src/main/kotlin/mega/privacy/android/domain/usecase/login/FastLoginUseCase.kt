package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for fast login.
 */
class FastLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param session Account session.
     * @param refreshChatUrl True if should refresh chat api URL, false otherwise.
     * @param disableChatApiUseCase [DisableChatApiUseCase]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        session: String,
        refreshChatUrl: Boolean,
        disableChatApiUseCase: DisableChatApiUseCase,
    ) = callbackFlow {
        loginMutex.lock()

        runCatching { initialiseMegaChatUseCase(session) }
            .onFailure { exception ->
                if (exception is ChatNotInitializedErrorStatus) {
                    chatLogoutUseCase(disableChatApiUseCase)
                }
            }

        if (refreshChatUrl) {
            loginRepository.refreshMegaChatUrl()
        }

        runCatching {
            loginRepository.fastLoginFlow(session)
                .collectLatest { loginStatus ->
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        saveAccountCredentialsUseCase()
                        loginMutex.unlock()
                    }

                    trySend(loginStatus)
                }
        }.onFailure {
            if (it !is LoginLoggedOutFromOtherLocation) {
                chatLogoutUseCase(disableChatApiUseCase)
                resetChatSettingsUseCase()
            }

            loginMutex.unlock()
            throw it
        }

        awaitClose {
            loginMutex.unlock()
        }
    }
}