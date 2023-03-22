package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Initialise MegaChat
 */
class InitialiseMegaChatUseCase @Inject constructor(private val loginRepository: LoginRepository) {

    /**
     * Invoke method
     *
     * @param session Required account session.
     */
    suspend operator fun invoke(session: String) = loginRepository.initMegaChat(session)
}