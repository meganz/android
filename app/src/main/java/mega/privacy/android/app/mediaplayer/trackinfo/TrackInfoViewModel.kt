package mega.privacy.android.app.mediaplayer.trackinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.GetAudioNodeByHandleUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for track (audio node) info UI logic.
 */
@HiltViewModel
class TrackInfoViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val getAudioNodeByHandleUseCase: GetAudioNodeByHandleUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val getNodeLocationInfoUseCase: GetNodeLocationInfo,
) : ViewModel() {
    private val _state = MutableStateFlow(TrackInfoState())
    internal val state = _state.asStateFlow()

    internal fun loadTrackInfo(handle: Long) {
        viewModelScope.launch {
            loadNodeInfo(handle)
        }
    }

    private suspend fun loadNodeInfo(handle: Long) =
        getAudioNodeByHandleUseCase(handle, false)?.let { audioNode ->
            val location = getNodeLocationInfoUseCase(audioNode)
            val thumbnail = audioNode.thumbnailPath?.let { path -> File(path) }
            val size = fileSizeStringMapper(audioNode.size)
            val durationString = durationInSecondsTextMapper(audioNode.duration)
            _state.update {
                it.copy(
                    thumbnail = thumbnail,
                    availableOffline = audioNode.isAvailableOffline,
                    size = size,
                    location = location,
                    added = audioNode.creationTime,
                    lastModified = audioNode.modificationTime,
                    durationString = durationString
                )
            }
        } ?: Timber.e("Failed to get audio node by handle: $handle")

    /**
     * Make a node available offline, or remove it from offline.
     */
    fun makeAvailableOffline(handle: Long) =
        viewModelScope.launch {
            val audioNode = getAudioNodeByHandleUseCase(handle, false) ?: return@launch
            if (isAvailableOfflineUseCase(audioNode)) {
                removeOfflineNode(handle)
            } else {
                _state.update {
                    it.copy(
                        transferTriggerEvent = triggered(
                            TransferTriggerEvent.StartDownloadForOffline(audioNode)
                        ),
                        availableOffline = true,
                    )
                }
            }
        }

    private suspend fun removeOfflineNode(handle: Long) {
        runCatching { removeOfflineNodeUseCase(NodeId(handle)) }
            .onSuccess {
                loadNodeInfo(handle)
                _state.update {
                    it.copy(
                        offlineRemovedEvent = triggered
                    )
                }
            }
            .onFailure {
                Timber.e(it)
            }
    }


    /**
     * Get latest value of StorageState
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()

    /**
     * Consume transfer trigger event
     */
    fun consumeTransferEvent() {
        _state.update {
            it.copy(transferTriggerEvent = consumed())
        }
    }

    /**
     * Consume offline removed event
     */
    fun consumeOfflineRemovedEvent() {
        _state.update {
            it.copy(
                offlineRemovedEvent = consumed
            )
        }
    }
}
