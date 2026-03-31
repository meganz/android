package mega.privacy.android.feature.cloudexplorer.presentation.favouritesexplorer

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerSharedViewModel.Args
import mega.privacy.android.shared.nodes.mapper.NodeUiItemMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import timber.log.Timber

@HiltViewModel(assistedFactory = FavouritesExplorerViewModel.Factory::class)
class FavouritesExplorerViewModel @AssistedInject constructor(
    monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase: MonitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    nodeViewItemMapper: NodeViewItemMapper,
    private val getAllFavoritesUseCase: GetAllFavoritesUseCase,
    @Assisted private val args: Args,
) : NodeExplorerSharedViewModel(
    monitorNodeUpdatesByIdUseCase = monitorNodeUpdatesByIdUseCase,
    monitorStorageStateUseCase = monitorStorageStateUseCase,
    monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
    monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
    nodeViewItemMapper = nodeViewItemMapper,
    args = Args(
        nodeId = NodeId(-1),
        nodeSourceType = NodeSourceType.FAVOURITES,
    ),
) {

    private val _favouritesExplorerInternalUiState = MutableStateFlow(FavouritesExplorerUiState())
    val favouritesExplorerUiState = _favouritesExplorerInternalUiState.asStateFlow()

    init {
        _favouritesExplorerInternalUiState.update { state -> state.copy(showFiles = args.showFiles) }
        loadNodes()
    }

    override fun loadNodes() {
        viewModelScope.launch {
            getAllFavoritesUseCase()
                .catch { Timber.e(it) }
                .collectLatest { nodes ->
                    val favourites =
                        if (args.showFiles) nodes else nodes.filterIsInstance<TypedFolderNode>()

                    setItems(
                        nodes = favourites,
                        nodesLoadingState = NodesLoadingState.FullyLoaded,
                    )
                }
        }
    }

    override fun refreshNodes() {
        loadNodes()
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): FavouritesExplorerViewModel
    }

    data class Args(
        val showFiles: Boolean,
    )
}
