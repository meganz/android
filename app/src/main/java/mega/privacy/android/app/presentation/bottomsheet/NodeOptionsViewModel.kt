package mega.privacy.android.app.presentation.bottomsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.app.presentation.bottomsheet.model.NodeShareInformation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * [ViewModel] associated with [NodeOptionsBottomSheetDialogFragment]
 *
 * @property createShareKey [CreateShareKey]
 * @property getNodeByHandle [GetNodeByHandle]
 * @property isNodeDeletedFromBackupsUseCase [IsNodeDeletedFromBackupsUseCase]
 * @property monitorConnectivityUseCase [MonitorConnectivityUseCase]
 * @property removeOfflineNodeUseCase [RemoveOfflineNodeUseCase]
 */
@HiltViewModel
class NodeOptionsViewModel @Inject constructor(
    private val createShareKey: CreateShareKey,
    private val getNodeByHandle: GetNodeByHandle,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val shareKeyCreated = MutableStateFlow<Boolean?>(null)

    private val _state = MutableStateFlow(NodeBottomSheetUIState())

    /**
     * The UI State for [NodeOptionsBottomSheetDialogFragment]
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                savedStateHandle.getStateFlow(NODE_ID_KEY, -1L)
                    .map { getNodeByHandle(it) },
                savedStateHandle.getStateFlow(SHARE_DATA_KEY, null),
                shareKeyCreated,
                monitorConnectivityUseCase(),
            ) { node: MegaNode?, shareData: NodeShareInformation?, shareKeyCreated: Boolean?, isOnline: Boolean ->
                { state: NodeBottomSheetUIState ->
                    state.copy(
                        node = node,
                        shareData = shareData,
                        shareKeyCreated = shareKeyCreated,
                        isOnline = isOnline,
                    )
                }
            }.collect {
                _state.update(it)
            }
        }
    }

    /**
     * Updates the UI State when the "Move" option is selected
     *
     * @param clicked true if the option is clicked, and false if otherwise
     */
    fun setMoveNodeClicked(clicked: Boolean) =
        _state.update { it.copy(canMoveNode = clicked) }

    /**
     * Updates the UI State when the "Restore" option is selected
     *
     * @param clicked true if the option is clicked, and false if otherwise
     */
    fun setRestoreNodeClicked(clicked: Boolean) = viewModelScope.launch {
        _state.value.node?.let { nonNullNode ->
            val isNodeDeletedFromBackups =
                isNodeDeletedFromBackupsUseCase(NodeId(nonNullNode.handle))
            if (isNodeDeletedFromBackups) {
                _state.update { it.copy(canMoveNode = clicked) }
            } else {
                _state.update { it.copy(canRestoreNode = clicked) }
            }
        }
    }

    /**
     * Creates a Shared Key
     */
    fun createShareKey() {
        viewModelScope.launch {
            kotlin.runCatching {
                val node = state.value.node
                    ?: throw IllegalArgumentException("Cannot create a share key for a null node")
                createShareKey(node)
            }.onSuccess {
                shareKeyCreated.emit(true)
            }.onFailure {
                shareKeyCreated.emit(false)
            }
        }
    }

    /**
     * Change the value of shareKeyCreated to false after it is consumed.
     */
    fun shareDialogDisplayed() {
        shareKeyCreated.tryEmit(null)
    }

    /**
     * Detect the file whether could be previewed directly
     *
     * @param node MegaNode
     * @return true is that the file could be previewed directly, otherwise is false
     */
    fun isFilePreviewOnline(node: MegaNode): Boolean =
        MimeTypeList.typeForName(node.name).let {
            it.isAudio || it.isVideoMimeType
        }


    /**
     * Remove offline node
     */
    fun removeOfflineNode(handle: Long) {
        viewModelScope.launch {
            runCatching { removeOfflineNodeUseCase(NodeId(handle)) }
                .onFailure {
                    Timber.e(it)
                }
        }
    }

    companion object {
        /**
         * The Mode Key
         */
        const val MODE_KEY = "MODE"

        /**
         * The Node ID Key
         */
        const val NODE_ID_KEY = "NODE_ID_KEY"

        /**
         * The Share Data Key
         */
        const val SHARE_DATA_KEY = "SHARE_DATA_KEY"
    }
}