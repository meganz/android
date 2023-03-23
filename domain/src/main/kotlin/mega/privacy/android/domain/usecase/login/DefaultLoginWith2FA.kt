package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginWrongMultiFactorAuth
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import javax.inject.Inject

/**
 * Default implementation of [LoginWith2FA].
 */
class DefaultLoginWith2FA @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatLogout: ChatLogout,
    private val resetChatSettings: ResetChatSettings,
    private val saveAccountCredentials: SaveAccountCredentials,
    @LoginMutex private val loginMutex: Mutex,
) : LoginWith2FA {

    override fun invoke(
        email: String,
        password: String,
        pin2FA: String,
        disableChatApi: DisableChatApi,
    ) = callbackFlow {
        loginMutex.lock()

        runCatching {
            loginRepository.multiFactorAuthLogin(email, password, pin2FA)
                .collectLatest { loginStatus ->
                    if (loginStatus == LoginStatus.LoginSucceed) {
                        saveAccountCredentials()
                        loginMutex.unlock()
                    }

                    trySend(loginStatus)
                }
        }.onFailure {
            if (it !is LoginLoggedOutFromOtherLocation
                && it !is LoginWrongMultiFactorAuth
            ) {
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