package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default implementation of [ChatLogout]
 */
class DefaultChatLogout @Inject constructor(
    private val loginRepository: LoginRepository,
    private val loggingRepository: LoggingRepository,
) : ChatLogout {

    override suspend fun invoke(disableChatApi: DisableChatApi) {
        kotlin.runCatching { loginRepository.chatLogout() }
            .onSuccess {
                disableChatApi()
                loggingRepository.resetSdkLogging()
            }
    }
}