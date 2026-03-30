package mega.privacy.android.feature.clouddrive.presentation.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchParameters
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.search.SearchUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioAction
import mega.privacy.android.feature.clouddrive.presentation.audio.model.AudioUiState
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeViewItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val searchUseCase: SearchUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
) : ViewModel() {

    private val openedFileNode = MutableStateFlow<TypedFileNode?>(null)

    private val audioSearchParameters: SearchParameters
        get() = SearchParameters(
            query = "",
            searchTarget = SearchTarget.ROOT_NODES,
            searchCategory = SearchCategory.AUDIO,
        )

    private fun hiddenNodesFlow() = combine(
        monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
        monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
        ::Pair
    )

    private fun searchRevampFlow() = flow {
        emit(
            runCatching { getFeatureFlagValueUseCase(AppFeatures.SearchRevamp) }
                .onFailure { Timber.e(it) }
                .getOrDefault(false)
        )
    }

    private fun nodeUpdatesTriggerFlow() = merge(
        monitorNodeUpdatesUseCase().map { }.catch { Timber.e(it) },
        monitorOfflineNodeUpdatesUseCase().map { }.catch { Timber.e(it) },
    ).onStart { emit(Unit) }

    private fun audioItemsFlow() = combine(
        monitorSortCloudOrderUseCase().catch { Timber.e(it) }.filterNotNull(),
        nodeUpdatesTriggerFlow(),
    ) { sortOrder, _ -> sortOrder }
        .flatMapLatest { sortOrder ->
            flow {
                runCatching {
                    val nodes = searchUseCase(
                        parentHandle = NodeId(-1),
                        nodeSourceType = NodeSourceType.AUDIO,
                        searchParameters = audioSearchParameters,
                        isSingleActivityEnabled = true
                    )
                    val nodeViewItems = nodeViewItemMapper(
                        nodeList = nodes,
                        nodeSourceType = NodeSourceType.AUDIO,
                    )
                    emit(nodeViewItems to sortOrder)
                }.onFailure {
                    Timber.e(it)
                    emit(emptyList<NodeViewItem<TypedNode>>() to sortOrder)
                }
            }
        }

    val uiState by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorViewTypeUseCase().catch { Timber.e(it); emit(ViewType.LIST) },
            audioItemsFlow(),
            openedFileNode,
            searchRevampFlow(),
            hiddenNodesFlow(),
        ) { viewType, itemsAndSort, opened, searchRevamp, hiddenNodes ->
            val (items, sortOrder) = itemsAndSort
            val (isHiddenNodesEnabled, showHiddenNodes) = hiddenNodes
            AudioUiState.Data(
                items = items,
                currentViewType = viewType,
                openedFileNode = opened,
                selectedSortOrder = sortOrder,
                selectedSortConfiguration = nodeSortConfigurationUiMapper(sortOrder),
                isSearchRevampEnabled = searchRevamp,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                showHiddenNodes = showHiddenNodes,
            )
        }.asUiStateFlow(
            viewModelScope,
            AudioUiState.Loading,
        )
    }

    /**
     * Process AudioAction and call relevant methods
     */
    fun processAction(action: AudioAction) {
        when (action) {
            is AudioAction.ItemClicked -> onItemClicked(action.node as? TypedFileNode)
            is AudioAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is AudioAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
        }
    }

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }

    /**
     * Handle item click - open the audio file
     */
    private fun onItemClicked(fileNode: TypedFileNode?) {
        fileNode ?: return
        openedFileNode.value = fileNode
    }

    /**
     * This method will toggle node view type between list and grid.
     */
    private fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val currentViewType = when (val state = uiState.value) {
                    is AudioUiState.Data -> state.currentViewType
                    is AudioUiState.Loading -> ViewType.LIST
                }
                val toggledViewType = when (currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggledViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    /**
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        openedFileNode.value = null
    }
}
