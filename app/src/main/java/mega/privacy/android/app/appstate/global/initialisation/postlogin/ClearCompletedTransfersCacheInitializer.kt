package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.transfers.completed.ClearCompletedTransfersCacheUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import timber.log.Timber
import javax.inject.Inject

/**
 * Post login initializer to monitor node updates and clear completed transfers cache when a folder node is moved.
 * Completed transfers cache needs to be cleared in that case because the cached values depends on folder hierarchy, so if the hierarchy is changed it needs to be filled again.
 * @property monitorNodeUpdatesUseCase
 * @property clearCompletedTransfersCacheUseCase
 */
class ClearCompletedTransfersCacheInitializer @Inject constructor(
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    clearCompletedTransfersCacheUseCase: ClearCompletedTransfersCacheUseCase,
) : PostLoginInitialiserAction({ _, _ ->
    monitorNodeUpdatesUseCase()
        .catch {
            Timber.e(it, "Error monitoring node updates in ClearCompletedTransfersCacheInitializer")
        }
        .map { nodeUpdates ->
            nodeUpdates.changes.any { (node, changes) ->
                node is FolderNode && changes.any { it == NodeChanges.Parent }
            }
        }.collect {
            if (it) {
                Timber.d("Completed transfers cache cleaned")
                clearCompletedTransfersCacheUseCase()
            }
        }
})