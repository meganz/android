package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import javax.inject.Inject

/**
 * Background fast login use case.
 * This fast login does not require to show the login screen.
 * A complete fast login process includes three different requests in this order:
 *      1.- initMegaChat
 *      2.- fastLogin
 *      3.- fetchNodes
 * Until all of them have been completed, a new login will not be possible.
 * If this is broken at some point, then the app can suffer unexpected behaviors like
 * logout and lose the current user's session.
 */
class BackgroundFastLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val getRootNodeExistsUseCase: RootNodeExistsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke(): String {
        loginMutex.withLock {
            val session =
                getSessionUseCase() ?: throw SessionNotRetrievedException()
            if (!getRootNodeExistsUseCase()) {
                initialiseMegaChatUseCase(session)
                loginRepository.fastLogin(session)
                loginRepository.fetchNodes()
                // return new session
                return getSessionUseCase().orEmpty()
            }
            return session
        }
    }
}