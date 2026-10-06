package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.login.FetchNodesBlockedAccount
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import javax.inject.Inject

/**
 * Use case for fetching nodes.
 */
class FetchNodesUseCase @Inject constructor(
    private val loginRepository: LoginRepository,
    private val resetChatSettingsUseCase: ResetChatSettingsUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    @LoginMutex private val loginMutex: Mutex,
) {

    /**
     * Invoke.
     *
     * Another login flow, e.g. a background fast login, may finish fetching nodes while this one
     * waits for the [loginMutex]. Fetching nodes again on an already loaded account makes the SDK
     * discard its local cache and download the whole account, so it is skipped unless
     * [isRefreshSession] is true.
     *
     * @param isRefreshSession True if the SDK requested a reload and nodes must be fetched again.
     * @return Flow of [FetchNodesUpdate].
     */
    operator fun invoke(isRefreshSession: Boolean = false) = callbackFlow {
        runCatching {
            loginMutex.lock()
            if (!isRefreshSession && rootNodeExistsUseCase()) {
                trySend(FetchNodesUpdate(progress = Progress(1F)))
                close()
                return@runCatching
            }
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
