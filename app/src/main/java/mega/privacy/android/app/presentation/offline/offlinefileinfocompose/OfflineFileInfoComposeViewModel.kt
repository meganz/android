package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFileTotalSizeUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineFolderInformationUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationByIdUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View Model class for [OfflineFileInfoComposeFragment]
 */
@HiltViewModel
internal class OfflineFileInfoComposeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getOfflineNodeInformationByIdUseCase: GetOfflineNodeInformationByIdUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val isImageFileUseCase: IsImageFileUseCase,
    private val getOfflineFolderInformationUseCase: GetOfflineFolderInformationUseCase,
    private val getOfflineFileTotalSizeUseCase: GetOfflineFileTotalSizeUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
) : ViewModel() {

    /**
     * Node Id from [SavedStateHandle]
     */
    private val nodeId = NodeId(savedStateHandle.get<Long>(NODE_HANDLE) ?: -1L)

    /**
     * private mutable UI state
     */
    private val _state = MutableStateFlow(OfflineFileInfoUiState())

    /**
     * public immutable UI State for view
     */
    val state = _state.asStateFlow()

    init {
        loadOfflineNodeInformation()
    }

    private fun loadOfflineNodeInformation() {
        viewModelScope.launch {
            runCatching {
                getOfflineNodeInformationByIdUseCase(nodeId)?.let { offlineNodeInfo ->
                    val offlineFile = getOfflineFileUseCase(offlineNodeInfo)
                    val totalSize = getOfflineFileTotalSizeUseCase(offlineFile)
                    val folderInfo = getFolderInfoOrNull(offlineNodeInfo)
                    val thumbnail = getThumbnailPathOrNull(offlineNodeInfo.isFolder, offlineFile)
                    val addedTime = offlineNodeInfo.lastModifiedTime?.div(1000L)

                    _state.update {
                        it.copy(
                            title = offlineNodeInfo.name,
                            isFolder = offlineNodeInfo.isFolder,
                            addedTime = addedTime,
                            totalSize = totalSize,
                            folderInfo = folderInfo,
                            thumbnail = thumbnail
                        )
                    }
                } ?: run {
                    handleError()
                }
            }.onFailure {
                handleError()
                Timber.e(it)
            }
        }
    }

    private fun handleError() {
        _state.update {
            it.copy(
                errorEvent = triggered(true)
            )
        }
    }

    private suspend fun getThumbnailPathOrNull(
        isFolder: Boolean,
        offlineFile: File,
    ): String? {
        if (isFolder) return null

        val isImage = isImageFileUseCase(offlineFile.absolutePath)
        return (if (isImage) offlineFile else getThumbnailUseCase(nodeId.longValue))
            ?.takeIf { it.exists() }
            ?.toURI()
            ?.toString()
    }

    private suspend fun getFolderInfoOrNull(
        offlineNodeInfo: OfflineNodeInformation,
    ) = offlineNodeInfo.takeIf { it.isFolder }?.let {
        getOfflineFolderInformationUseCase(it.id)
    }

    /**
     * Remove the node from database and cache storage
     */
    fun removeFromOffline() {
        viewModelScope.launch {
            runCatching {
                removeOfflineNodeUseCase(nodeId)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    fun onErrorEventConsumed() =
        _state.update { state -> state.copy(errorEvent = consumed()) }

    companion object {
        const val NODE_HANDLE = "handle"
    }
}

