package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import javax.inject.Inject

/**
 * Default implementation of [Login].
 */
class DefaultLogin @Inject constructor(
    private val loginRepository: LoginRepository,
    private val chatLogout: ChatLogout,
    private val resetChatSettings: ResetChatSettings,
    private val saveAccountCredentials: SaveAccountCredentials,
    @LoginMutex private val loginMutex: Mutex,
) : Login {

    override fun invoke(email: String, password: String, disableChatApi: DisableChatApi) =
        flow {
            loginMutex.withLock {
                runCatching { loginRepository.initMegaChat() }
                    .onFailure { exception ->
                        when (exception) {
                            is ChatNotInitializedErrorStatus -> {
                                chatLogout(disableChatApi)
                            }
                            is ChatNotInitializedUnknownStatus -> {
                                emit(LoginStatus.LoginCannotStart)
                                return@flow
                            }
                        }

                    }

                runCatching {
                    emitAll(loginRepository.login(email, password))
                }.onSuccess {
                    saveAccountCredentials()
                }.onFailure {
                    if (it !is LoginLoggedOutFromOtherLocation
                        && it !is LoginMultiFactorAuthRequired
                    ) {
                        chatLogout(disableChatApi)
                        resetChatSettings()
                    }

                    throw it
                }
            }
        }
}