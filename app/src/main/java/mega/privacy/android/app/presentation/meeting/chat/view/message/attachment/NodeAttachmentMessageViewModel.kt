package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.ImportNodesResult
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.usecase.chat.GetShareChatNodesUseCase
import mega.privacy.android.domain.usecase.chat.message.GetCachedOriginalPathUseCase
import mega.privacy.android.domain.usecase.chat.message.GetMessageIdsByTypeUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.ImportTypedNodesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * View model for node attachment message. This view model manages all visible node attachment messages
 * in a chat room.
 *
 */
@HiltViewModel
class NodeAttachmentMessageViewModel @Inject constructor(
    private val getPreviewUseCase: GetPreviewUseCase,
    private val getMessageIdsByTypeUseCase: GetMessageIdsByTypeUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getCachedOriginalPathUseCase: GetCachedOriginalPathUseCase,
    private val isAvailableOfflineUseCase: IsAvailableOfflineUseCase,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    private val nodeShareContentUrisIntentMapper: NodeShareContentUrisIntentMapper,
    private val getShareChatNodesUseCase: GetShareChatNodesUseCase,
    private val importTypedNodesUseCase: ImportTypedNodesUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    fileSizeStringMapper: FileSizeStringMapper,
    fileTypeIconMapper: FileTypeIconMapper,
    durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : AbstractAttachmentMessageViewModel<NodeAttachmentMessage>(
    fileSizeStringMapper,
    durationInSecondsTextMapper,
    fileTypeIconMapper
) {
    override fun onMessageAdded(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        attachmentMessage: NodeAttachmentMessage,
    ) {
        updatePreview(mutableStateFlow, attachmentMessage)
    }

    private fun updatePreview(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        nodeAttachmentMessage: NodeAttachmentMessage,
    ) {
        viewModelScope.launch {
            val node = nodeAttachmentMessage.fileNode
            if (node.hasPreview && mutableStateFlow.value.previewUri == null) {
                runCatching {
                    getPreviewUseCase(node)
                }.onSuccess { previewFile ->
                    mutableStateFlow.update {
                        it.copy(
                            previewUri = previewFile?.toString(),
                            loadProgress = null,
                        )
                    }
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    override fun createFirstUiState(attachmentMessage: NodeAttachmentMessage): AttachmentMessageUiState =
        with(super.createFirstUiState(attachmentMessage)) {
            when (attachmentMessage.fileType) {
                is ImageFileTypeInfo, is VideoFileTypeInfo -> {
                    copy(previewUri = getCachedOriginalPathUseCase(attachmentMessage.fileNode))
                }

                else -> {
                    this
                }
            }
        }

    suspend fun useImagePreview() = getFeatureFlagValueUseCase(AppFeatures.ImagePreview)

    /**
     * Handle file node
     *
     * @param message node attachment message
     */
    suspend fun handleFileNode(message: NodeAttachmentMessage): FileNodeContent {
        val fileNode = message.fileNode
        return when {
            fileNode.type is ImageFileTypeInfo -> FileNodeContent.ImageForChat(
                allAttachmentMessageIds = getNodeAttachmentMessageIds(message.chatId)
            )

            fileNode.type is TextFileTypeInfo && fileNode.size <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> FileNodeContent.TextContent

            fileNode.type is PdfFileTypeInfo -> FileNodeContent.Pdf(
                uri = getChatNodeContentUri(message)
            )

            fileNode.type is VideoFileTypeInfo || fileNode.type is AudioFileTypeInfo -> FileNodeContent.AudioOrVideo(
                uri = getChatNodeContentUri(message)
            )

            else -> FileNodeContent.Other(
                localFile = getNodePreviewFileUseCase(message.fileNode)
            )
        }
    }

    /**
     * Get message ids by type
     *
     * @param chatId chat id
     */
    suspend fun getNodeAttachmentMessageIds(chatId: Long) =
        getMessageIdsByTypeUseCase(chatId, ChatMessageType.NODE_ATTACHMENT)

    /**
     * Get chat node content uri
     *
     * @param message node attachment message
     */
    suspend fun getChatNodeContentUri(message: NodeAttachmentMessage) =
        getNodeContentUriUseCase(
            fileNode = message.fileNode
        )

    /**
     * Apply node content uri
     *
     */
    fun applyNodeContentUri(
        intent: Intent,
        content: NodeContentUri,
        mimeType: String,
        isSupported: Boolean = true,
    ) {
        nodeContentUriIntentMapper(intent, content, mimeType, isSupported)
    }


    /**
     * Is available offline
     *
     * @param node
     */
    suspend fun isAvailableOffline(node: TypedNode) = isAvailableOfflineUseCase(node)

    /**
     * Remove offline node
     *
     * @param node
     */
    suspend fun removeOfflineNode(node: TypedNode) = removeOfflineNodeUseCase(node.id)

    /**
     * Get share chat nodes
     *
     * @param fileNodes list of chat file nodes
     */
    suspend fun getShareChatNodes(fileNodes: List<ChatFile>) =
        getShareChatNodesUseCase(fileNodes)

    /**
     * Get share intent
     *
     * @param fileNodes list of chat file nodes
     * @param content node share content uri
     * @return intent
     */
    fun getShareIntent(fileNodes: List<ChatFile>, content: NodeShareContentUri): Intent {
        val node = fileNodes.first()
        val groupMimeType = if (fileNodes.all { it.type.mimeType == node.type.mimeType }) {
            node.type.mimeType
        } else {
            "*/*"
        }
        val title = if (fileNodes.size > 1) {
            "${UUID.randomUUID()}.url"
        } else {
            node.name
        }
        return nodeShareContentUrisIntentMapper(title, content, groupMimeType)
    }

    /**
     * Import nodes.
     */
    suspend fun importNodes(
        nodes: List<TypedNode>,
        handleWhereToImport: Long,
    ) = importTypedNodesUseCase(nodes, handleWhereToImport)

    /**
     * Get copy nodes result.
     */
    fun getCopyNodesResult(result: ImportNodesResult) = with(result) {
        copyRequestMessageMapper(
            CopyRequestResult(
                copySuccess + copyError,
                copyError
            )
        )
    }
}