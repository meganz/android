package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import javax.inject.Inject

/**
 * Complete fast login use case.
 * This fast login does not require to show the login screen.
 * A complete fast login process includes three different requests in this order:
 *      1.- initMegaChat
 *      2.- fastLogin
 *      3.- fetchNodes
 * Until all of them have been completed, a new login will not be possible.
 * If this is broken at some point, then the app can suffer unexpected behaviors like
 * logout and lose the current user's session.
 *
 * Note that this use case does not check if the root node exists. Use only this use case
 * in context where the session and the root node have been previously checked.
 */
class CompleteFastLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @param session Account session.
     */
    suspend operator fun invoke(session: String) {
        loginMutex.withLock {
            initialiseMegaChatUseCase(session)
            loginRepository.fastLogin(session)
            loginRepository.fetchNodes()
        }
    }
}