package mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.usecase.GetNodeInfoByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.GetNodeNavigationStackUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.nodes.mapper.NodeSourceTypeToSearchTargetMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = NodesExplorerViewModel.Factory::class)
class NodesExplorerViewModel @AssistedInject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    nodeViewItemMapper: NodeViewItemMapper,
    getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase,
    private val getNodeInfoByIdUseCase: GetNodeInfoByIdUseCase,
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase,
    searchUseCase: SearchUseCase,
    nodeSourceTypeToSearchTargetMapper: NodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase: GetNodeNavigationStackUseCase,
    @Assisted private val args: Args,
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    nodeViewItemMapper = nodeViewItemMapper,
    getContactVerificationWarningUseCase = getContactVerificationWarningUseCase,
    searchUseCase = searchUseCase,
    nodeSourceTypeToSearchTargetMapper = nodeSourceTypeToSearchTargetMapper,
    getNodeNavigationStackUseCase = getNodeNavigationStackUseCase,
    args = args,
) {

    override val nodesFlow: Flow<NodesResult> = refreshSignal
        .map { true }
        .onStart { emit(false) }
        .flatMapLatest { isRefresh ->
            if (isRefresh) {
                flow {
                    emit(
                        NodesResult(
                            nodes = getFileBrowserNodeChildrenUseCase(args.nodeId.longValue),
                            loadingState = NodesLoadingState.FullyLoaded,
                        )
                    )
                }.catch { Timber.e(it) }
            } else {
                getNodesByIdInChunkUseCase(args.nodeId)
                    .map { (nodes, hasMore) ->
                        NodesResult(
                            nodes = nodes,
                            loadingState = if (hasMore) {
                                NodesLoadingState.PartiallyLoaded
                            } else {
                                NodesLoadingState.FullyLoaded
                            },
                        )
                    }
                    .catch { Timber.e(it) }
            }
        }

    override val folderNameFlow: Flow<LocalizedText> = flow { emit(folderName()) }

    override val isRootNodeFlow: Flow<Boolean> = flow { emit(isRoot()) }

    private suspend fun folderName(): LocalizedText {
        val nodeInfo = runCatching { getNodeInfoByIdUseCase(args.nodeId) }
            .onFailure { Timber.e(it, "Failed to get node name for title update") }
            .getOrNull()
        return if (nodeInfo?.isNodeKeyDecrypted == false) {
            LocalizedText.StringRes(resId = NodesR.string.shared_items_verify_credentials_undecrypted_folder)
        } else {
            LocalizedText.Literal(nodeInfo?.name ?: "")
        }
    }

    private suspend fun isRoot(): Boolean {
        val rootId = runCatching { getRootNodeIdUseCase() }.onFailure { Timber.e(it) }.getOrNull()
        return rootId == null || rootId == args.nodeId
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): NodesExplorerViewModel
    }
}
