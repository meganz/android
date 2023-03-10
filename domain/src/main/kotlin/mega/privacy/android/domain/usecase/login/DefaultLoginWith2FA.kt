package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
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
    ) = loginRepository.multiFactorAuthLogin(email, password, pin2FA)
        .onStart { loginMutex.lock() }
        .onCompletion { exception ->
            if (exception == null) {
                saveAccountCredentials()
            }

            loginMutex.unlock()
        }.catch {
            if (it !is LoginLoggedOutFromOtherLocation
                && it !is LoginWrongMultiFactorAuth
            ) {
                chatLogout(disableChatApi)
                resetChatSettings()
            }

            throw it
        }
}