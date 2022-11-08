package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.exception.LoginAlreadyRunningException
import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default implementation of [CompleteFastLogin].
 *
 * @property loginRepository [LoginRepository].
 */
class DefaultCompleteFastLogin @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChat: InitialiseMegaChat,
) : CompleteFastLogin {

    override suspend fun invoke(session: String) {
        if (loginRepository.isLoginAlreadyRunning()) {
            throw LoginAlreadyRunningException()
        }

        loginRepository.startLoginProcess()

        runCatching { initialiseMegaChat(session) }
            .onFailure { exception -> finishWithException(exception) }
            .onSuccess {
                runCatching { loginRepository.fastLogin(session) }
                    .onFailure { exception -> finishWithException(exception) }
                    .onSuccess {
                        runCatching { loginRepository.fetchNodes() }
                            .onFailure { exception -> finishWithException(exception) }
                            .onSuccess { loginRepository.finishLoginProcess() }
                    }
            }
    }

    private fun finishWithException(exception: Throwable) {
        loginRepository.finishLoginProcess()
        throw  exception
    }
}