package mega.privacy.android.app.mediaplayer.trackinfo

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeLocationInfo
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.utils.OfflineUtils.saveOffline
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedAudioNode
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
    fun makeAvailableOffline(handle: Long, activity: FragmentActivity) =
        viewModelScope.launch {
            val audioNode = getAudioNodeByHandleUseCase(handle, false) ?: return@launch
            if (isAvailableOfflineUseCase(audioNode)) {
                removeOfflineNode(handle)
            } else {
                saveOfflineNode(handle = handle, audioNode = audioNode, activity = activity)
            }
        }

    private suspend fun removeOfflineNode(handle: Long) {
        runCatching { removeOfflineNodeUseCase(NodeId(handle)) }
            .onSuccess {
                loadNodeInfo(handle)
                _state.update {
                    it.copy(
                        offlineRemoveSnackBarShow = true
                    )
                }
            }
            .onFailure {
                Timber.e(it)
            }
    }

    private suspend fun saveOfflineNode(
        handle: Long,
        audioNode: TypedAudioNode,
        activity: FragmentActivity,
    ) {
        val node = getNodeByHandle(handle) ?: return
        val offlineParent = getOfflinePathForNodeUseCase(audioNode) ?: return
        val parentFile = File(offlineParent)
        if (parentFile.exists().not()) {
            parentFile.mkdirs()
        }

        // we need call legacy code OfflineUtils.saveOffline to save node for offline, which require
        // activity :(
        saveOffline(parentFile, node, activity)
        _state.update {
            it.copy(
                availableOffline = true,
                offlineRemoveSnackBarShow = false
            )
        }
    }


    /**
     * Get latest value of StorageState
     */
    fun getStorageState() = monitorStorageStateEventUseCase.getState()
}
