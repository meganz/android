package mega.privacy.android.app.presentation.transfers.starttransfer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to be shared by legacy screens where there isn't a view model to handle download state following our architecture (like [NodeOptionsBottomSheetDialogFragment] and its callers)
 * It can receive download events through a state flow instead of of directly calling legacy download methods
 * Once these screens are migrated to compose this view model should be removed and the state added to the corresponding view model.
 */
@HiltViewModel
@Deprecated(message = "This view model should be used only as a temporal solution for existing legacy screens and view models, new view models should have the [TypedNode] and handle the [TransferTriggerEvent] event in its ui state")
class StartDownloadViewModel @Inject constructor(
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val getPublicChildNodeFromIdUseCase: GetPublicChildNodeFromIdUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val downloadNodesUseCase: DownloadNodesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<StateEventWithContent<TransferTriggerEvent>>(consumed())

    /**
     * State flow that emits [TransferTriggerEvent]s
     */
    val state = _state.asStateFlow()

    /**
     * Triggers the event related to download a node with [nodeId]
     * @param nodeId
     */
    fun onDownloadClicked(nodeId: NodeId, isHighPriority: Boolean = false) {
        viewModelScope.launch {
            val nodes = listOfNotNull(getNodeByIdUseCase(nodeId))
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes, isHighPriority))
            }
        }
    }

    /**
     * Triggers the event related to download a node for preview with its [nodeId]
     */
    fun onDownloadForPreviewClicked(nodeId: NodeId) {
        viewModelScope.launch {
            val node = getNodeByIdUseCase(nodeId)
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadForPreview(node))
            }
        }
    }

    /**
     * Triggers the event related to download nods with [nodeIds]
     * @param nodeIds
     */
    fun onDownloadClicked(nodeIds: List<NodeId>, isHighPriority: Boolean = false) {
        viewModelScope.launch {
            val nodes = nodeIds.mapNotNull { getNodeByIdUseCase(it) }
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes, isHighPriority))
            }
        }
    }

    /**
     * On copy offline node clicked
     *
     * @param nodeIds
     */
    fun onCopyOfflineNodeClicked(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            _state.update {
                triggered(TransferTriggerEvent.CopyOfflineNode(nodeIds))
            }
        }
    }

    /**
     * On copy uri clicked
     *
     * @param name of the file
     * @param uri of the file
     */
    fun onCopyUriClicked(name: String, uri: Uri) {
        viewModelScope.launch {
            _state.update {
                triggered(TransferTriggerEvent.CopyUri(name, uri))
            }
        }
    }

    /**
     * Triggers the event to download a folder link's child node with its [nodeId]
     * @param nodeId
     */
    fun onFolderLinkChildNodeDownloadClicked(nodeId: NodeId) {
        viewModelScope.launch {
            val nodes = listOfNotNull(getPublicChildNodeFromIdUseCase(nodeId))
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes))
            }
        }
    }

    /**
     * Triggers the event related to download a node with its serialized data
     * @param serializedData of the link node
     */
    fun onDownloadClicked(serializedData: String) {
        viewModelScope.launch {
            val nodes = listOfNotNull(getPublicNodeFromSerializedDataUseCase(serializedData))
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes))
            }
        }
    }

    /**
     * Triggers the event related to download multiple nodes with their serialized data
     * @param serializedData of the link node
     */
    fun onMultipleSerializedNodesDownloadClicked(
        serializedData: List<String>,
        isHighPriority: Boolean = false,
    ) {
        viewModelScope.launch {
            val nodes = serializedData.mapNotNull { getPublicNodeFromSerializedDataUseCase(it) }
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes, isHighPriority))
            }
        }
    }

    /**
     * Triggers the event related to download a chat node
     * @param chatId
     * @param messageId
     */
    fun onDownloadClicked(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            getChatFileUseCase(chatId, messageId)?.let { chatFile ->
                _state.update {
                    triggered(TransferTriggerEvent.StartDownloadNode(listOf(chatFile)))
                }
            }
        }
    }

    /**
     * Triggers the event related to download a list of chat nodes (all from the same chat)
     * @param chatId
     * @param messageIds
     */
    fun onDownloadClicked(chatId: Long, messageIds: List<Long>) {
        viewModelScope.launch {
            val chatFiles =
                messageIds.mapNotNull { getChatFileUseCase(chatId, it) }
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(chatFiles))
            }
        }
    }

    /**
     * Triggers the event related to download a node
     * @param node
     */
    fun onDownloadClicked(node: TypedNode) {
        _state.update {
            triggered(TransferTriggerEvent.StartDownloadNode(listOf(node)))
        }
    }

    /**
     * Triggers the event related to save offline a node with [nodeId]
     * @param nodeId
     */
    fun onSaveOfflineClicked(nodeId: NodeId) {
        viewModelScope.launch {
            val node = getNodeByIdUseCase(nodeId)
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadForOffline(node))
            }
        }
    }

    /**
     * Triggers the event related to save offline a chat node
     * @param chatId
     * @param messageId
     */
    fun onSaveOfflineClicked(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            getChatFileUseCase(chatId, messageId)?.let { chatFile ->
                _state.update {
                    triggered(TransferTriggerEvent.StartDownloadForOffline(chatFile))
                }
            }
        }
    }

    /**
     * Triggers the event to save offline a node with its serialized data for link nodes
     * @param serializedData of the link node
     */
    fun onSaveOfflineClicked(serializedData: String) {
        viewModelScope.launch {
            val node = getPublicNodeFromSerializedDataUseCase(serializedData)
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadForOffline(node))
            }
        }
    }

    /**
     * Triggers the event related to save offline a node
     * @param node
     */
    fun onSaveOfflineClicked(node: TypedNode) {
        _state.update {
            triggered(TransferTriggerEvent.StartDownloadForOffline(node))
        }
    }

    /**
     * Download a voice clip
     */
    @Deprecated(message = "This method will be removed once ChatActivity has been removed")
    fun downloadVoiceClip(name: String, chatId: Long, messageId: Long) {
        viewModelScope.launch {
            val node = getChatFileUseCase(chatId, messageId) ?: run {
                Timber.e("voice clip node not found")
                return@launch
            }
            val voiceClipFile: File =
                getCacheFileUseCase(CacheFolderManager.VOICE_CLIP_FOLDER, name) ?: run {
                    Timber.e("voice clip cache path is invalid ($name - $chatId - $messageId)")
                    return@launch
                }
            downloadNodesUseCase(
                nodes = listOf(node),
                destinationPath = "${voiceClipFile.parent}${File.separator}",
                appData = TransferAppData.VoiceClip,
                isHighPriority = true,
            ).lastOrNull()
        }
    }

    /**
     * consume the event once it's processed by the view
     */
    fun consumeDownloadEvent() {
        _state.update { consumed() }
    }

    /**
     * Triggers the event related to upload files.
     */
    fun onUploadClicked(event: TransferTriggerEvent.StartUpload) {
        _state.update {
            triggered(event)
        }
    }
}