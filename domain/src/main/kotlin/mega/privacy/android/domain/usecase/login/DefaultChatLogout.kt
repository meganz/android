package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.ResetSdkLogger
import javax.inject.Inject

/**
 * Default implementation of [ChatLogout]
 */
class DefaultChatLogout @Inject constructor(
    private val loginRepository: LoginRepository,
    private val resetSdkLogger: ResetSdkLogger,
) : ChatLogout {

    override suspend fun invoke(disableChatApi: DisableChatApi) {
        kotlin.runCatching { loginRepository.chatLogout() }
            .onSuccess {
                disableChatApi()
                resetSdkLogger()
            }
    }
}