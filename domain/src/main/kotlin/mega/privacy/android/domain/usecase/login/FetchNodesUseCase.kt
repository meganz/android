package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.login.FetchNodesBlockedAccount
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for fetching nodes.
 */
class FetchNodesUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * @return Flow of [FetchNodesUpdate].
     */
    operator fun invoke() = callbackFlow {
        runCatching {
            loginMutex.lock()
            loginRepository.fetchNodesFlow()
                .catch {
                    if (it !is FetchNodesBlockedAccount) {
                        resetChatSettingsUseCase()
                    }
                    close(it)
                }
                .collectLatest { update ->
                    if (update.progress?.floatValue == 1F) {
                        runCatching { loginMutex.unlock() }
                    }
                    trySend(update)
                }
        }.onFailure {
            close(it)
        }
        awaitClose {
            runCatching { loginMutex.unlock() }
        }
    }
}
