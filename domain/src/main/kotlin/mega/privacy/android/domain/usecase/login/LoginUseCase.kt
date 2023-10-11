package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for logging.
 */
class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param email Account email.
     * @param password Account password.
     * @param disableChatApiUseCase [DisableChatApiUseCase]
     * @return Flow of [LoginStatus].
     */
    operator fun invoke(
        email: String,
        password: String,
        disableChatApiUseCase: DisableChatApiUseCase,
    ) = callbackFlow {
        loginMutex.lock()

        runCatching { loginRepository.initMegaChat() }
            .onFailure { exception ->
                when (exception) {
                    is ChatNotInitializedErrorStatus -> {
                        chatLogoutUseCase(disableChatApiUseCase)
                    }
                    is ChatNotInitializedUnknownStatus -> {
                        trySend(LoginStatus.LoginCannotStart)
                        runCatching { loginMutex.unlock() }
                        return@callbackFlow
                    }
                }

            }

        runCatching {
            loginRepository.login(email, password).collectLatest { loginStatus ->
                if (loginStatus == LoginStatus.LoginSucceed) {
                    saveAccountCredentialsUseCase()
                    runCatching { loginMutex.unlock() }
                }

                trySend(loginStatus)
            }
        }.onFailure {
            if (it !is LoginLoggedOutFromOtherLocation
                && it !is LoginMultiFactorAuthRequired
            ) {
                chatLogoutUseCase(disableChatApiUseCase)
                resetChatSettingsUseCase()
            }

            runCatching { loginMutex.unlock() }
            throw it
        }

        awaitClose {
            runCatching { loginMutex.unlock() }
        }
    }
}