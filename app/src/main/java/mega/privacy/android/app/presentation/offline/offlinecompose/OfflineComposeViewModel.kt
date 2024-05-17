package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUiState
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.shared.original.core.ui.utils.pop
import mega.privacy.android.shared.original.core.ui.utils.push
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileTotalSizeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderInformationUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineComposeViewModel of [OfflineFragmentCompose]
 */
@HiltViewModel
class OfflineComposeViewModel @Inject constructor(
    private val getOfflineNodesByParentIdUseCase: GetOfflineNodesByParentIdUseCase,
    private val monitorTransfersFinishedUseCase: MonitorTransfersFinishedUseCase,
    private val setOfflineWarningMessageVisibilityUseCase: SetOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineWarningMessageVisibilityUseCase: MonitorOfflineWarningMessageVisibilityUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val offlineFolderInformationUseCase: GetOfflineFolderInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val getOfflineFileTotalSizeUseCase: GetOfflineFileTotalSizeUseCase,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUiState())
    private val parentStack = ArrayDeque<Pair<Int, String>>()

    /**
     * Flow of [OfflineUiState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorOfflineWarningMessage()
        monitorOfflineNodeUpdates()
        monitorViewTypeUpdate()
        viewModelScope.launch {
            monitorTransfersFinishedUseCase().conflate().collect {
                refreshList()
            }
        }
    }

    private fun monitorViewTypeUpdate() {
        viewModelScope.launch {
            monitorViewType()
                .catch {
                    Timber.e(it)
                }
                .collect { viewType ->
                    _uiState.update {
                        it.copy(
                            currentViewType = viewType
                        )
                    }
                }
        }
    }

    private fun monitorOfflineWarningMessage() {
        viewModelScope.launch {
            monitorOfflineWarningMessageVisibilityUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect {
                    _uiState.update { state -> state.copy(showOfflineWarning = it) }
                }
        }
    }

    private fun monitorOfflineNodeUpdates() {
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase()
                .catch {
                    Timber.e(it)
                }
                .conflate()
                .collect {
                    refreshList()
                }
        }
    }

    private fun refreshList() {
        viewModelScope.launch {
            runCatching {
                getOfflineNodesByParentIdUseCase(uiState.value.parentId)
            }.onSuccess {
                val offlineNodeUiList = it?.map { offlineInfo ->
                    OfflineNodeUIItem(offlineNode = loadOfflineNodeInformation(offlineInfo))
                } ?: emptyList()
                _uiState.update {
                    it.copy(
                        offlineNodes = offlineNodeUiList
                    )
                }
            }.onFailure {
                Timber.e(it)
                _uiState.update {
                    it.copy(offlineNodes = emptyList())
                }
            }
        }
    }

    /**
     * Dismiss showing the Offline warning message
     */
    fun dismissOfflineWarning() {
        viewModelScope.launch {
            setOfflineWarningMessageVisibilityUseCase(isVisible = false)
        }
    }

    private suspend fun loadOfflineNodeInformation(offlineNodeInfo: OfflineNodeInformation): OfflineFileInfoUiState {
        val offlineFile = getOfflineFileUseCase(offlineNodeInfo)
        val totalSize = getOfflineFileTotalSizeUseCase(offlineFile)
        val folderInfo = getFolderInfoOrNull(offlineNodeInfo)
        val addedTime = offlineNodeInfo.lastModifiedTime?.div(1000L)
        val thumbnail = runCatching {
            getThumbnailPathOrNull(
                isFolder = offlineNodeInfo.isFolder,
                handle = offlineNodeInfo.handle.toLong(),
            )
        }.getOrElse {
            Timber.e(it)
            null
        }

        return OfflineFileInfoUiState(
            id = offlineNodeInfo.id,
            handle = offlineNodeInfo.handle.toLong(),
            title = offlineNodeInfo.name,
            isFolder = offlineNodeInfo.isFolder,
            addedTime = addedTime,
            totalSize = totalSize,
            folderInfo = folderInfo,
            thumbnail = thumbnail
        )
    }

    private suspend fun getThumbnailPathOrNull(
        isFolder: Boolean,
        handle: Long,
    ): String? {
        if (isFolder) return null

        return getThumbnailUseCase(handle)
            ?.takeIf { it.exists() }
            ?.toURI()
            ?.toString()
    }

    private suspend fun getFolderInfoOrNull(
        offlineNodeInfo: OfflineNodeInformation,
    ) = offlineNodeInfo.takeIf { it.isFolder }?.let {
        offlineFolderInformationUseCase(it.id)
    }

    /**
     * Handle on clicked of item
     */
    fun onItemClicked(offlineNodeUIItem: OfflineNodeUIItem) {
        if (uiState.value.selectedNodeHandles.isEmpty()) {
            if (offlineNodeUIItem.offlineNode.isFolder) {
                parentStack.push(
                    Pair(
                        offlineNodeUIItem.offlineNode.parentId,
                        uiState.value.title
                    )
                )
                _uiState.update {
                    it.copy(
                        title = offlineNodeUIItem.offlineNode.title,
                        parentId = offlineNodeUIItem.offlineNode.id,
                    )
                }
                refreshList()
            }
        } else {
            onLongItemClicked(offlineNodeUIItem)
        }
    }

    /**
     * OnBackClicked
     */
    fun onBackClicked(): Int? {
        parentStack.pop()?.let { (parentId, parentTitle) ->
            _uiState.update {
                it.copy(
                    parentId = parentId,
                    title = parentTitle
                )
            }
            refreshList()
            return null
        } ?: run {
            return -1
        }
    }

    /**
     * On Long item clicked
     * @param offlineNodeUIItem [OfflineNodeUIItem]
     */
    fun onLongItemClicked(offlineNodeUIItem: OfflineNodeUIItem) {
        val index =
            _uiState.value.offlineNodes.indexOfFirst { it.offlineNode.handle == offlineNodeUIItem.offlineNode.handle }
        updateNodeInSelectionState(offlineNodeUIItem = offlineNodeUIItem, index = index)
    }

    private fun updateNodeInSelectionState(offlineNodeUIItem: OfflineNodeUIItem, index: Int) {
        offlineNodeUIItem.isSelected = !offlineNodeUIItem.isSelected
        val newNodesList =
            _uiState.value.offlineNodes.updateItemAt(index = index, item = offlineNodeUIItem)
        val selectedHandleList = newNodesList.filter {
            it.isSelected
        }.map {
            it.offlineNode.handle
        }
        _uiState.update {
            it.copy(
                offlineNodes = newNodesList,
                selectedNodeHandles = selectedHandleList,
            )
        }
    }

    /**
     * Clear Selected nodes
     */
    fun clearSelection() {
        val clearList = _uiState.value.offlineNodes.map {
            it.copy(isSelected = false)
        }
        _uiState.update {
            it.copy(
                offlineNodes = clearList,
                selectedNodeHandles = emptyList()
            )
        }
    }

    /**
     * Select All nodes
     */
    fun selectAll() {
        viewModelScope.launch {
            val selectedList = _uiState.value.offlineNodes.map {
                it.copy(isSelected = true)
            }
            val selectedListHandles = selectedList.map { it.offlineNode.handle }
            _uiState.update {
                it.copy(
                    offlineNodes = selectedList,
                    selectedNodeHandles = selectedListHandles
                )
            }
        }
    }
}