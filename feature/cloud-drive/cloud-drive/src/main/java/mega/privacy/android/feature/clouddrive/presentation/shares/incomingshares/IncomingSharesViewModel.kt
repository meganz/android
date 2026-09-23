package mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.FolderPreferenceKeys
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import mega.privacy.android.domain.usecase.contact.GetContactVerificationWarningUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.MonitorFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderSortOrderUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderViewTypeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorOthersSortOrderUseCase
import mega.privacy.android.domain.usecase.shares.GetIncomingSharesChildrenNodeUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesUiState
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.nodes.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.shared.nodes.mapper.NodeViewItemMapper
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
    private val getIncomingSharesChildrenNodeUseCase: GetIncomingSharesChildrenNodeUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val monitorFolderViewTypeUseCase: MonitorFolderViewTypeUseCase,
    private val setFolderViewTypeUseCase: SetFolderViewTypeUseCase,
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase,
    private val setFolderSortOrderUseCase: SetFolderSortOrderUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val nodeViewItemMapper: NodeViewItemMapper,
    private val monitorOthersSortOrderUseCase: MonitorOthersSortOrderUseCase,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val getContactVerificationWarningUseCase: GetContactVerificationWarningUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomingSharesUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        monitorViewType()
        monitorSortOrder()
        monitorNodeUpdates()
    }

    /**
     * Process incoming shares actions and call relevant methods
     */
    fun processAction(action: IncomingSharesAction) {
        when (action) {
            is IncomingSharesAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is IncomingSharesAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
        }
    }

    private suspend fun loadNodes(sortOrder: SortOrder) {
        val folderId = uiState.value.currentFolderId
        runCatching {
            val isContactVerificationOn = runCatching {
                getContactVerificationWarningUseCase()
            }.getOrDefault(false)
            val nodes = getIncomingSharesChildrenNodeUseCase(
                parentHandle = folderId.longValue,
                sortOrder = sortOrder,
            )
            val nodeItems = nodeViewItemMapper(
                nodeList = nodes,
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                isContactVerificationOn = isContactVerificationOn,
                highlightedNodeId = null,
                highlightedNames = null,
                isHiddenNodesEnabled = false,
            )
            _uiState.update { state ->
                state.copy(
                    items = nodeItems,
                    isLoading = false,
                )
            }
        }.onFailure {
            Timber.e(it)
            _uiState.update { state -> state.copy(isLoading = false) }
            snackbarEventQueue.queueMessage(sharedR.string.general_request_failed_message)
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(
                nodeId = NodeId(-1L),
                nodeSourceType = NodeSourceType.INCOMING_SHARES
            ).collectLatest { change ->
                if (change == NodeChanges.Remove) {
                    // If current folder is moved to rubbish bin, navigate back
                    _uiState.update {
                        it.copy(navigateBack = triggered)
                    }
                } else {
                    loadNodes(uiState.value.selectedSortOrder)
                }
            }
        }
    }

    /**
     * Consume navigate back event
     */
    private fun onNavigateBackEventConsumed() {
        _uiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    /**
     * This method will toggle node view type between list and grid.
     */
    private fun onChangeViewTypeClicked() {
        viewModelScope.launch {
            runCatching {
                val toggledViewType = when (uiState.value.currentViewType) {
                    ViewType.LIST -> ViewType.GRID
                    ViewType.GRID -> ViewType.LIST
                }
                setFolderViewTypeUseCase(
                    folderKey = FolderPreferenceKeys.INCOMING_SHARES,
                    viewType = toggledViewType,
                    currentSortOrder = uiState.value.selectedSortOrder,
                    orElse = { setViewTypeUseCase(it) },
                )
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorFolderViewTypeUseCase(
                folderKey = FolderPreferenceKeys.INCOMING_SHARES,
                orElse = monitorViewTypeUseCase(),
            )
                .catch { Timber.e(it) }
                .collect { viewType ->
                    _uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    private fun monitorSortOrder() {
        viewModelScope.launch {
            monitorFolderSortOrderUseCase(
                folderKey = FolderPreferenceKeys.INCOMING_SHARES,
                orElse = monitorOthersSortOrderUseCase().filterNotNull(),
            )
                .distinctUntilChanged()
                .catch { Timber.e(it) }
                .collectLatest { sortOrder ->
                    _uiState.update {
                        it.copy(
                            selectedSortConfiguration = nodeSortConfigurationUiMapper(sortOrder),
                            selectedSortOrder = sortOrder,
                        )
                    }
                    loadNodes(sortOrder)
                }
        }
    }

    /**
     * Set sort order
     */
    fun setSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setFolderSortOrderUseCase(
                    folderKey = FolderPreferenceKeys.INCOMING_SHARES,
                    sortOrder = order,
                    currentViewType = uiState.value.currentViewType,
                    orElse = { setOthersSortOrder(it) },
                )
            }.onFailure {
                Timber.e(it, "Failed to set sort order")
            }
        }
    }

}
