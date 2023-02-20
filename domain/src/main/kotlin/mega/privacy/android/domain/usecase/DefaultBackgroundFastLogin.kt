package mega.privacy.android.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * Default implementation of [BackgroundFastLogin].
 *
 * @property loginRepository [LoginRepository].
 */
class DefaultBackgroundFastLogin @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChat: InitialiseMegaChat,
    private val getSession: GetSession,
    private val getRootNodeExists: RootNodeExists,
    @LoginMutex private val loginMutex: Mutex,
) : BackgroundFastLogin {
    override suspend fun invoke(): String {
        loginMutex.withLock {
            val session =
                getSession() ?: throw SessionNotRetrievedException()
            if (!getRootNodeExists()) {
                initialiseMegaChat(session)
                loginRepository.fastLogin(session)
                loginRepository.fetchNodes()
                // return new session
                return getSession().orEmpty()
            }
            return session
        }
    }
}