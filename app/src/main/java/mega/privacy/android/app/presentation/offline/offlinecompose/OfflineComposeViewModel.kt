package mega.privacy.android.app.presentation.offline.offlinecompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineNodeUIItem
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUIState
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.usecase.GetOfflineNodesByParentIdUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileTotalSizeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderInformationUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.offline.SetOfflineWarningMessageVisibilityUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransfersFinishedUseCase
import timber.log.Timber
import java.io.File
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineUIState())

    /**
     * Flow of [OfflineUIState] UI State
     */
    val uiState = _uiState.asStateFlow()

    init {
        monitorOfflineWarningMessage()
        monitorOfflineNodeUpdates()
        viewModelScope.launch {
            monitorTransfersFinishedUseCase().conflate().collect {
                refreshList()
            }
        }
    }

    private fun monitorOfflineWarningMessage() {
        viewModelScope.launch {
            monitorOfflineWarningMessageVisibilityUseCase().collect {
                _uiState.update { state -> state.copy(showOfflineWarning = it) }
            }
        }
    }

    private fun monitorOfflineNodeUpdates() {
        viewModelScope.launch {
            monitorOfflineNodeUpdatesUseCase().conflate()
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
                } ?: run {
                    emptyList()
                }
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
     * Offline folder clicked
     */
    fun onFolderClicked(parentId: Int) {
        _uiState.update {
            it.copy(parentId = parentId)
        }
        refreshList()
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
}