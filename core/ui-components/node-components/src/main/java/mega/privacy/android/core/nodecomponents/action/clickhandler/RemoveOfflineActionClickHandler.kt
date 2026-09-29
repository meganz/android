package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.transfers.offline.CancelAndDeleteOfflineDownloadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Removes the nodes from offline, cancelling their download first when it is still in flight, so
 * the action also undoes an offline download that has not finished yet.
 */
class RemoveOfflineActionClickHandler @Inject constructor(
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val cancelAndDeleteOfflineDownloadUseCase: CancelAndDeleteOfflineDownloadUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveOfflineMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    if (getFeatureFlagValueUseCase(ApiFeatures.OfflineDownloadInProgressState)) {
                        cancelAndDeleteOfflineDownloadUseCase(node)
                    }
                    removeOfflineNodeUseCase(nodeId = node.id)
                }.onFailure { Timber.e(it) }
                    .onSuccess {
                        provider.viewModel.onRemoveFromOfflineSuccess(1)
                    }
            }
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    if (getFeatureFlagValueUseCase(ApiFeatures.OfflineDownloadInProgressState)) {
                        // each cancellation waits for its transfers to stop, so they run
                        // together and the whole selection waits only once
                        nodes.map { node ->
                            async { cancelAndDeleteOfflineDownloadUseCase(node) }
                        }.awaitAll()
                    }
                    nodes.forEach { node ->
                        removeOfflineNodeUseCase(nodeId = node.id)
                    }
                }.onFailure { Timber.e(it) }
                    .onSuccess {
                        provider.viewModel.onRemoveFromOfflineSuccess(nodes.size)
                    }
            }
        }
    }
}