package mega.privacy.android.shared.nodes.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import timber.log.Timber

/**
 * ViewModel for the node explorer header: view type (list/grid) and sort configuration.
 * Use [Factory] to create with [NodeSourceType]; for INCOMING_SHARES, sort uses
 * [GetOthersSortOrder]/[SetOthersSortOrder], otherwise [SetCloudSortOrder] and monitor flow.
 */
@HiltViewModel(assistedFactory = NodeHeaderItemViewModel.Factory::class)
class NodeHeaderItemViewModel @AssistedInject constructor(
    private val monitorViewTypeUseCase: MonitorViewType,
    private val setViewTypeUseCase: SetViewType,
    private val monitorSortCloudOrderUseCase: mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    @Assisted private val nodeSourceType: NodeSourceType,
) : ViewModel() {

    private val incomingSharesSortConfigFlow = MutableStateFlow<NodeSortConfiguration?>(null)

    val uiState: StateFlow<NodeHeaderItemUiState> by lazy {
        val viewTypeFlow = monitorViewTypeUseCase()
            .catch { Timber.e(it) }
        val sortConfigFlow = when (nodeSourceType) {
            NodeSourceType.INCOMING_SHARES -> incomingSharesSortConfigFlow
                .onStart {
                    emit(
                        runCatching { nodeSortConfigurationUiMapper(getOthersSortOrder()) }
                            .onFailure { Timber.e(it, "Failed to get others sort order") }
                            .getOrDefault(NodeSortConfiguration.default)
                    )
                }.filterNotNull()

            else -> monitorSortCloudOrderUseCase()
                .catch { Timber.e(it) }
                .filterNotNull()
                .map { nodeSortConfigurationUiMapper(it) }
        }
        combine(
            viewTypeFlow,
            sortConfigFlow,
        ) { viewType, nodeSortConfiguration ->
            NodeHeaderItemUiState.Data(
                viewType = viewType,
                nodeSortConfiguration = nodeSortConfiguration,
            )
        }.asUiStateFlow(
            scope = viewModelScope,
            initialValue = NodeHeaderItemUiState.Loading,
        )
    }

    fun updateViewType() {
        viewModelScope.launch {
            runCatching {
                val viewType = (uiState.value as NodeHeaderItemUiState.Data).viewType
                val toggled = when (viewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setViewTypeUseCase(toggled)
                toggled
            }.onFailure { Timber.e(it, "Failed to change view type") }
        }
    }

    fun updateNodeSortConfiguration(nodeSortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(nodeSortConfiguration)
                when (nodeSourceType) {
                    NodeSourceType.INCOMING_SHARES -> {
                        setOthersSortOrder(order)
                        incomingSharesSortConfigFlow.value = nodeSortConfiguration
                    }

                    else -> setCloudSortOrderUseCase(order)
                }
            }.onFailure { Timber.e(it, "Failed to set sort order") }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(nodeSourceType: NodeSourceType): NodeHeaderItemViewModel
    }
}
