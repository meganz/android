package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import javax.inject.Inject

/**
 * Default implementation of [Login].
 */
class DefaultFastLogin @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChat: InitialiseMegaChat,
    private val chatLogout: ChatLogout,
    private val resetChatSettings: ResetChatSettings,
    private val saveAccountCredentials: SaveAccountCredentials,
    @LoginMutex private val loginMutex: Mutex,
) : FastLogin {

    override fun invoke(
        session: String,
        refreshChatUrl: Boolean,
        disableChatApi: DisableChatApi,
    ): Flow<LoginStatus> = callbackFlow {
        loginMutex.lock()

        runCatching { initialiseMegaChat(session) }
            .onFailure { exception ->
                if (exception is ChatNotInitializedErrorStatus) {
                    chatLogout(disableChatApi)
                }
            }

        if (refreshChatUrl) {
            loginRepository.refreshMegaChatUrl()
        }

        runCatching {
            loginRepository.fastLoginFlow(session)
                .collectLatest { loginStatus ->
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        saveAccountCredentials()
                        loginMutex.unlock()
                    }

                    trySend(loginStatus)
                }
        }.onFailure {
            if (it !is LoginLoggedOutFromOtherLocation) {
                chatLogout(disableChatApi)
                resetChatSettings()
            }

            loginMutex.unlock()
            throw it
        }

        awaitClose {
            loginMutex.unlock()
        }
    }
}