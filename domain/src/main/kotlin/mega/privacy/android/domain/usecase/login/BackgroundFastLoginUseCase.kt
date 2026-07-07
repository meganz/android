package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import javax.inject.Inject

/**
 * Background fast login use case.
 * This fast login does not require to show the login screen.
 * A complete fast login process includes three different requests:
 *      1.- initMegaChat and fastLogin, which can run in parallel
 *      2.- fetchNodes, which requires both of them to have finished
 * Until all of them have been completed, a new login will not be possible.
 * If this is broken at some point, then the app can suffer unexpected behaviors like
 * logout and lose the current user's session.
 */
class BackgroundFastLoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChatUseCase: InitialiseMegaChatUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val getRootNodeExistsUseCase: RootNodeExistsUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke(): String {
        try {
            loginMutex.lock()

            val session = getSessionUseCase() ?: run {
                throw SessionNotRetrievedException()
            }

            if (!getRootNodeExistsUseCase()) {
                // MegaChat::init() can run in parallel with fastLogin, but fetchNodes must
                // not be invoked until both have finished because the SDK listener is
                // registered at the end of MegaChat::init(). coroutineScope guarantees that
                // ordering: it does not return until the launched init job has completed.
                coroutineScope {
                    launch { initialiseMegaChatUseCase(session) }
                    loginRepository.fastLogin(session)
                }
                // pre-fetch user data for API feature flag, we don't care about the result here
                runCatching { getUserDataUseCase() }
                loginRepository.fetchNodes()
                // return new session
                return getSessionUseCase().orEmpty()
            }

            return session
        } catch (e: Exception) {
            throw e
        } finally {
            runCatching { loginMutex.unlock() }
        }
    }
}