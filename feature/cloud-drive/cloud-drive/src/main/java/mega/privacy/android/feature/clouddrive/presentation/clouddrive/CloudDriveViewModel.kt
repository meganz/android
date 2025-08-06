package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.filebrowser.GetFileBrowserNodeChildrenUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesByIdUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import mega.privacy.android.domain.usecase.viewtype.SetViewType
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveUiState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloudDriveViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFileBrowserNodeChildrenUseCase: GetFileBrowserNodeChildrenUseCase,
    private val setViewTypeUseCase: SetViewType,
    private val monitorViewTypeUseCase: MonitorViewType,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val monitorNodeUpdatesByIdUseCase: MonitorNodeUpdatesByIdUseCase,
    private val nodeUiItemMapper: NodeUiItemMapper,
    private val scannerHandler: ScannerHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<CloudDrive>()
    internal val uiState: StateFlow<CloudDriveUiState>
        field = MutableStateFlow(CloudDriveUiState(currentFolderId = NodeId(args.nodeHandle)))

    init {
        monitorViewType()
        monitorAccountDetail()
        monitorHiddenNodes()
        viewModelScope.launch {
            loadNodes()
        }
        monitorNodeUpdates()
    }

    /**
     * Process CloudDriveAction and call relevant methods
     */
    fun processAction(action: CloudDriveAction) {
        when (action) {
            is CloudDriveAction.ItemClicked -> onItemClicked(action.nodeUiItem)
            is CloudDriveAction.ItemLongClicked -> onItemLongClicked(action.nodeUiItem)
            is CloudDriveAction.ChangeViewTypeClicked -> onChangeViewTypeClicked()
            is CloudDriveAction.OpenedFileNodeHandled -> onOpenedFileNodeHandled()
            is CloudDriveAction.SelectAllItems -> selectAllItems()
            is CloudDriveAction.DeselectAllItems -> deselectAllItems()
            is CloudDriveAction.SetHiddenNodesOnboarded -> setHiddenNodesOnboarded()
            is CloudDriveAction.NavigateToFolderEventConsumed -> onNavigateToFolderEventConsumed()
            is CloudDriveAction.NavigateBackEventConsumed -> onNavigateBackEventConsumed()
        }
    }

    private fun monitorNodeUpdates() {
        viewModelScope.launch {
            monitorNodeUpdatesByIdUseCase(NodeId(args.nodeHandle)).collectLatest {
                if (it == NodeChanges.Remove) {
                    // If current folder is moved to rubbish bin, navigate back
                    uiState.update {
                        it.copy(navigateBack = triggered)
                    }
                } else {
                    loadNodes()
                }
            }
        }
    }

    private fun monitorHiddenNodes() {
        viewModelScope.launch {
            if (isHiddenNodeFeatureFlagEnabled()) {
                checkIfHiddenNodeIsOnboarded()
                monitorShowHiddenNodesSettings()
            }
        }
    }

    private suspend fun isHiddenNodeFeatureFlagEnabled(): Boolean = runCatching {
        getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
    }.getOrDefault(false)

    private fun monitorShowHiddenNodesSettings() {
        monitorShowHiddenItemsUseCase()
            .conflate()
            .onEach { show ->
                uiState.update {
                    it.copy(showHiddenNodes = show)
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                if (isHiddenNodeFeatureFlagEnabled()) {
                    val accountType = accountDetail.levelDetail?.accountType
                    val isPaidAccount = accountType?.isPaid == true
                    val businessStatus = if (accountType?.isBusinessAccount == true) {
                        runCatching { getBusinessStatusUseCase() }.getOrNull()
                    } else null
                    val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
                    uiState.update {
                        it.copy(
                            isHiddenNodesEnabled = isPaidAccount && !isBusinessAccountExpired,
                        )
                    }
                }
            }
            .catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun checkIfHiddenNodeIsOnboarded() {
        viewModelScope.launch {
            runCatching {
                isHiddenNodesOnboardedUseCase()
            }.onSuccess { isHiddenNodesOnboarded ->
                uiState.update {
                    it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
                }
            }.onFailure {
                Timber.e(it, "Failed to check if hidden nodes are onboarded")
            }
        }
    }

    /**
     * Mark hidden nodes onboarding has shown
     */
    private fun setHiddenNodesOnboarded() {
        uiState.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }


    private suspend fun loadNodes() {
        val folderId = uiState.value.currentFolderId
        runCatching {
            getNodeByIdUseCase(folderId) to nodeUiItemMapper(
                nodeList = getFileBrowserNodeChildrenUseCase(folderId.longValue),
                nodeSourceType = args.nodeSourceType,
            )
        }.onSuccess { (currentNode, children) ->
            val title = LocalizedText.Literal(currentNode?.name ?: "")
            uiState.update { state ->
                state.copy(
                    title = title,
                    isLoading = false,
                    items = children,
                )
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * Handle item click - navigate to folder if it's a folder
     */
    private fun onItemClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        if (uiState.value.isInSelectionMode) {
            toggleItemSelection(nodeUiItem)
            return
        }
        when (nodeUiItem.node) {
            is TypedFolderNode -> {
                uiState.update { state ->
                    state.copy(
                        navigateToFolderEvent = triggered(nodeUiItem.id)
                    )
                }
            }

            is TypedFileNode -> {
                uiState.update { state ->
                    state.copy(
                        openedFileNode = nodeUiItem.node as TypedFileNode
                    )
                }
            }
        }
    }

    /**
     * Consume navigation event
     */
    private fun onNavigateToFolderEventConsumed() {
        uiState.update { state ->
            state.copy(navigateToFolderEvent = consumed())
        }
    }

    /**
     * Consume navigate back event
     */
    private fun onNavigateBackEventConsumed() {
        uiState.update { state ->
            state.copy(navigateBack = consumed)
        }
    }

    /**
     * Handle item long click - toggle selection state
     */
    private fun onItemLongClicked(nodeUiItem: NodeUiItem<TypedNode>) {
        toggleItemSelection(nodeUiItem)
    }

    private fun toggleItemSelection(nodeUiItem: NodeUiItem<TypedNode>) {
        val updatedItems = uiState.value.items.map { item ->
            if (item.node.id == nodeUiItem.node.id) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        uiState.update { state ->
            state.copy(items = updatedItems)
        }
    }

    /**
     * Deselect all items and reset selection state
     */
    private fun deselectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = false) }
        uiState.update { state ->
            state.copy(items = updatedItems)
        }
    }

    /**
     * Select all items
     */
    private fun selectAllItems() {
        val updatedItems = uiState.value.items.map { it.copy(isSelected = true) }
        uiState.update { state ->
            state.copy(items = updatedItems)
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
                setViewTypeUseCase(toggledViewType)
            }.onFailure {
                Timber.e(it, "Failed to change view type")
            }
        }
    }

    private fun monitorViewType() {
        viewModelScope.launch {
            monitorViewTypeUseCase()
                .catch { Timber.e(it) }
                .collect { viewType ->
                    uiState.update { it.copy(currentViewType = viewType) }
                }
        }
    }

    /**
     * Handle the event when a file node is opened
     */
    private fun onOpenedFileNodeHandled() {
        uiState.update { state ->
            state.copy(openedFileNode = null)
        }
    }

    /**
     * Prepares the ML Kit Document Scanner from Google Play Services
     */
    fun prepareDocumentScanner() {
        viewModelScope.launch {
            runCatching {
                scannerHandler.prepareDocumentScanner()
            }.onSuccess { gmsDocumentScanner ->
                uiState.update { it.copy(gmsDocumentScanner = gmsDocumentScanner) }
            }.onFailure { exception ->
                uiState.update {
                    it.copy(
                        documentScanningError = if (exception is InsufficientRAMToLaunchDocumentScanner) {
                            DocumentScanningError.InsufficientRAM
                        } else {
                            DocumentScanningError.GenericError
                        }
                    )
                }
            }
        }
    }

    /**
     * When the system fails to open the ML Kit Document Scanner, display a generic error message
     */
    fun onDocumentScannerFailedToOpen() {
        uiState.update { it.copy(documentScanningError = DocumentScanningError.GenericError) }
    }

    /**
     * Resets the value of [CloudDriveUiState.gmsDocumentScanner]
     */
    fun onGmsDocumentScannerConsumed() {
        uiState.update { it.copy(gmsDocumentScanner = null) }
    }

    /**
     * Resets the value of [CloudDriveUiState.documentScanningError]
     */
    fun onDocumentScanningErrorConsumed() {
        uiState.update { it.copy(documentScanningError = null) }
    }
}
