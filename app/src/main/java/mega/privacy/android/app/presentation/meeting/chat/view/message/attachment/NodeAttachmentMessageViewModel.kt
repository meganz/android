package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import android.content.Intent
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.usecase.chat.GetChatNodeContentUriUseCase
import mega.privacy.android.domain.usecase.chat.message.GetCachedOriginalPathUseCase
import mega.privacy.android.domain.usecase.chat.message.GetMessageIdsByTypeUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFilePathUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import timber.log.Timber
import java.io.File
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
    private val getChatNodeContentUriUseCase: GetChatNodeContentUriUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFilePathUseCase: GetNodePreviewFilePathUseCase,
    private val getCachedOriginalPathUseCase: GetCachedOriginalPathUseCase,
    fileSizeStringMapper: FileSizeStringMapper,
    durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : AbstractAttachmentMessageViewModel<NodeAttachmentMessage>(
    fileSizeStringMapper,
    durationInSecondsTextMapper,
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

    /**
     * Handle file node
     *
     * @param message node attachment message
     */
    suspend fun handleFileNode(message: NodeAttachmentMessage): FileNodeContent {
        val fileNode = message.fileNode
        return when {
            fileNode.type is ImageFileTypeInfo -> FileNodeContent.Image(
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
                localFile = getNodePreviewFilePathUseCase(message.fileNode)?.let { File(it) }
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
        getChatNodeContentUriUseCase(
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
}