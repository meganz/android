package mega.privacy.android.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.exception.SessionNotRetrievedException
import mega.privacy.android.domain.repository.LoginRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [CompleteFastLogin].
 *
 * @property loginRepository [LoginRepository].
 */
@Singleton
class DefaultCompleteFastLogin @Inject constructor(
    private val loginRepository: LoginRepository,
    private val initialiseMegaChat: InitialiseMegaChat,
    private val getSession: GetSession,
    private val getRootNodeExists: RootNodeExists,
) : CompleteFastLogin {
    private val mutex = Mutex()

    override suspend fun invoke(): String {
        mutex.withLock {
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