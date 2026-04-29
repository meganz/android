package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffListViewModel @Inject constructor(
    private val monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val clearRecentlyUsedItemsUseCase: ClearRecentlyUsedItemsUseCase,
) : ViewModel() {

    private val _sortConfig = MutableStateFlow(NodeSortConfiguration.default)
    private val _uiState = MutableStateFlow(ContinueWhereLeftOffListUiState())

    val uiState: StateFlow<ContinueWhereLeftOffListUiState> by lazy {
        combine(
            monitorContinueWhereLeftOffItemsUseCase(limit = MAX_LIST_ITEMS),
            _sortConfig,
        ) { items, sort ->
            _uiState.update {
                it.copy(
                    items = items.sortedWith(sort.comparator()),
                    sortConfiguration = sort,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)

        _uiState.asUiStateFlow(viewModelScope, ContinueWhereLeftOffListUiState())
    }

    fun onItemClicked(nodeHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
            }.onSuccess { node ->
                node?.let { _uiState.update { it.copy(openNodeEvent = triggered(node)) } }
            }.onFailure {
                Timber.d(it)
            }
        }
    }

    fun onOpenNodeEventConsumed() {
        _uiState.update { it.copy(openNodeEvent = consumed()) }
    }

    fun updateSortConfiguration(configuration: NodeSortConfiguration) {
        _sortConfig.value = configuration
        _uiState.update { it.copy(sortConfiguration = configuration, showSortSheet = false) }
    }

    fun onChangeViewTypeClicked() {
        _uiState.update { current ->
            current.copy(
                currentViewType = when (current.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                },
            )
        }
    }

    fun showSortSheet() {
        _uiState.update { it.copy(showSortSheet = true) }
    }

    fun dismissSortSheet() {
        _uiState.update { it.copy(showSortSheet = false) }
    }

    fun showOptionsSheet() {
        _uiState.update { it.copy(showOptionsSheet = true) }
    }

    fun dismissOptionsSheet() {
        _uiState.update { it.copy(showOptionsSheet = false) }
    }

    fun clearAll() {
        _uiState.update { it.copy(showOptionsSheet = false) }
        viewModelScope.launch {
            runCatching { clearRecentlyUsedItemsUseCase() }
                .onFailure { Timber.e(it, "Failed to clear CWLO history") }
        }
    }

    companion object {
        private const val MAX_LIST_ITEMS = 50

        private fun NodeSortConfiguration.comparator(): Comparator<ContinueWhereLeftOffItem> {
            val base: Comparator<ContinueWhereLeftOffItem> = when (sortOption) {
                NodeSortOption.Name ->
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }

                else ->
                    compareBy { it.lastAccessedTimestamp }
            }
            return if (sortDirection == SortDirection.Descending) base.reversed() else base
        }
    }
}
