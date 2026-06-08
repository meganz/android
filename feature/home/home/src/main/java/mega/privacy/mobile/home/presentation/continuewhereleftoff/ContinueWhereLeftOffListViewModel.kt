package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetCurrentVersionNodeUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffItemsUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.MonitorContinueWhereLeftOffSortPreferenceUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.SetContinueWhereLeftOffSortUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ContinueWhereLeftOffListViewModel @Inject constructor(
    private val monitorContinueWhereLeftOffItemsUseCase: MonitorContinueWhereLeftOffItemsUseCase,
    private val monitorContinueWhereLeftOffSortPreferenceUseCase: MonitorContinueWhereLeftOffSortPreferenceUseCase,
    private val setContinueWhereLeftOffSortUseCase: SetContinueWhereLeftOffSortUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getCurrentVersionNodeUseCase: GetCurrentVersionNodeUseCase,
    private val clearRecentlyUsedItemsUseCase: ClearRecentlyUsedItemsUseCase,
    private val nameResolver: ContinueWhereLeftOffNameResolver,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private val uiAction = MutableStateFlow(UiAction())
    private val openNodeEventChannel =
        Channel<StateEventWithContent<TypedFileNode>>(Channel.BUFFERED)

    val uiState: StateFlow<ContinueWhereLeftOffListUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorContinueWhereLeftOffItemsUseCase(MAX_LIST_ITEMS)
                .transformLatest { items ->
                    emit(nameResolver.applyCachedNames(items))
                    if (nameResolver.resolveBlankNames(items)) {
                        emit(nameResolver.applyCachedNames(items))
                    }
                },
            monitorContinueWhereLeftOffSortPreferenceUseCase()
                .map { (field, direction) -> field.toNodeSortConfiguration(direction) },
            openNodeEventChannel.receiveAsFlow()
                .onStart { emit(consumed()) },
            uiAction,
            monitorConnectivityUseCase().catch { Timber.e(it) },
        ) { items, sortConfiguration, openNodeEvent, action, isConnected ->
            ContinueWhereLeftOffListUiState(
                items = items.sortedForDisplay(sortConfiguration),
                isLoading = false,
                isConnected = isConnected,
                openNodeEvent = openNodeEvent,
                sortConfiguration = sortConfiguration,
                showSortSheet = action.showSortSheet,
                showOptionsSheet = action.showOptionsSheet,
                currentViewType = action.currentViewType,
            )
        }.catch { e ->
            Timber.e(e, "Failed to load CWLO list items")
        }.asUiStateFlow(
            viewModelScope,
            ContinueWhereLeftOffListUiState(),
        )
    }

    fun onItemClicked(nodeHandle: Long, type: RecentlyUsedType) {
        viewModelScope.launch {
            runCatching {
                // Text files can be edited and saved back, which overwrites the cloud node with a
                // new version (new handle). The stored handle then points at the stale previous
                // version, so resolve the current version before opening to reflect the latest edit.
                if (type == RecentlyUsedType.TextEditor) {
                    getCurrentVersionNodeUseCase(NodeId(nodeHandle))
                } else {
                    getNodeByIdUseCase(NodeId(nodeHandle)) as? TypedFileNode
                }
            }.onSuccess { node ->
                node?.let { openNodeEventChannel.send(triggered(it)) }
            }.onFailure {
                Timber.d(it)
            }
        }
    }

    fun onOpenNodeEventConsumed() {
        openNodeEventChannel.trySend(consumed())
    }

    fun updateSortConfiguration(configuration: NodeSortConfiguration) {
        uiAction.update { it.copy(showSortSheet = false) }
        viewModelScope.launch {
            runCatching {
                setContinueWhereLeftOffSortUseCase(
                    sortField = configuration.sortOption.toCwloSortField(),
                    sortDirection = configuration.sortDirection,
                )
            }.onFailure { Timber.e(it, "Failed to persist CWLO sort preference") }
        }
    }

    fun onChangeViewTypeClicked() {
        uiAction.update { current ->
            current.copy(
                currentViewType = when (current.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                },
            )
        }
    }

    fun showSortSheet() {
        uiAction.update { it.copy(showSortSheet = true) }
    }

    fun dismissSortSheet() {
        uiAction.update { it.copy(showSortSheet = false) }
    }

    fun showOptionsSheet() {
        uiAction.update { it.copy(showOptionsSheet = true) }
    }

    fun dismissOptionsSheet() {
        uiAction.update { it.copy(showOptionsSheet = false) }
    }

    fun clearAll() {
        uiAction.update { it.copy(showOptionsSheet = false) }
        viewModelScope.launch {
            runCatching { clearRecentlyUsedItemsUseCase() }
                .onFailure { Timber.e(it, "Failed to clear CWLO history") }
        }
    }

    private data class UiAction(
        val showSortSheet: Boolean = false,
        val showOptionsSheet: Boolean = false,
        val currentViewType: ViewType = ViewType.LIST,
    )

    /**
     * Re-sorts items by their resolved [ContinueWhereLeftOffItem.title] when sorting by name.
     *
     * The persisted file name in the database can be blank (it is only stored on a best-effort
     * basis when a file is opened), so the database-level name sort is unreliable. By this point
     * blank titles have been resolved from the node, so sorting here uses the actual node name as
     * the sort source. Timestamp order comes correctly from the repository and is left untouched.
     */
    private fun List<ContinueWhereLeftOffItem>.sortedForDisplay(
        sortConfiguration: NodeSortConfiguration,
    ): List<ContinueWhereLeftOffItem> {
        if (sortConfiguration.sortOption != NodeSortOption.Name) return this
        val byTitle = compareBy(String.CASE_INSENSITIVE_ORDER) { item: ContinueWhereLeftOffItem ->
            item.title
        }
        return when (sortConfiguration.sortDirection) {
            SortDirection.Ascending -> sortedWith(byTitle)
            SortDirection.Descending -> sortedWith(byTitle.reversed())
        }
    }

    companion object {
        private const val MAX_LIST_ITEMS = 50

        private fun NodeSortOption.toCwloSortField(): ContinueWhereLeftOffSortField =
            when (this) {
                NodeSortOption.Name -> ContinueWhereLeftOffSortField.Name
                else -> ContinueWhereLeftOffSortField.Timestamp
            }

        private fun ContinueWhereLeftOffSortField.toNodeSortConfiguration(
            direction: SortDirection,
        ): NodeSortConfiguration = NodeSortConfiguration(
            sortOption = when (this) {
                ContinueWhereLeftOffSortField.Name -> NodeSortOption.Name
                ContinueWhereLeftOffSortField.Timestamp -> NodeSortOption.LastAccessed
            },
            sortDirection = direction,
        )
    }
}
