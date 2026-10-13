package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import javax.inject.Inject

/**
 * Emits the root node of the logged-in account once its nodes are loaded, and null otherwise.
 * Built on the SDK's fetch-nodes completion, so every login path counts, with or without UI.
 */
internal class MonitorLoggedInRootNodeUseCase @Inject constructor(
    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase,
    private val monitorLogoutUseCase: MonitorLogoutUseCase,
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<NodeId?> =
        monitorUserCredentialsUseCase()
            .map { it?.session != null }
            .distinctUntilChanged()
            .flatMapLatest { hasSavedSession -> if (hasSavedSession) loadedRootNode() else flowOf(null) }
            .distinctUntilChanged()

    // The fetch-nodes finish and logout flows have no replay and no buffer. Both collectors
    // subscribe before the first root read, so that no fetch-nodes finish is lost, and only send
    // into the conflated channel, so that an emitter never waits for a root read.
    private fun loadedRootNode(): Flow<NodeId?> = channelFlow {
        launch(start = CoroutineStart.UNDISPATCHED) {
            monitorFetchNodesFinishUseCase().collect { send(LoginSignal.NodesFetched) }
        }
        launch(start = CoroutineStart.UNDISPATCHED) {
            monitorLogoutUseCase().collect { if (it) send(LoginSignal.LoggedOut) }
        }
        send(LoginSignal.NodesFetched)
    }.conflate().map { signal ->
        when (signal) {
            LoginSignal.NodesFetched -> getRootNodeIdUseCase()
            LoginSignal.LoggedOut -> null
        }
    }

    private enum class LoginSignal { NodesFetched, LoggedOut }
}
