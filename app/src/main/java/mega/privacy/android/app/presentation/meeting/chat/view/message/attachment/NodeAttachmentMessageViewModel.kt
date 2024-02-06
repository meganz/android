package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.usecase.node.chat.AddChatFileTypeUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for node attachment message. This view model manages all visible node attachment messages
 * in a chat room.
 *
 */
@HiltViewModel
class NodeAttachmentMessageViewModel @Inject constructor(
    private val getPreviewUseCase: GetPreviewUseCase,
    private val addChatFileTypeUseCase: AddChatFileTypeUseCase,
    fileSizeStringMapper: FileSizeStringMapper,
    durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : AbstractAttachmentMessageViewModel<NodeAttachmentMessage>(
    fileSizeStringMapper,
    durationInSecondsTextMapper
) {
    override fun onMessageAdded(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        attachmentMessage: NodeAttachmentMessage,
        chatId: Long,
    ) {
        updatePreview(mutableStateFlow, attachmentMessage, chatId)
    }

    private fun updatePreview(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        nodeAttachmentMessage: NodeAttachmentMessage, chatId: Long,
    ) {
        viewModelScope.launch {
            val node = nodeAttachmentMessage.fileNode
            val msgId = nodeAttachmentMessage.msgId
            if (node.hasPreview && mutableStateFlow.value.previewUri == null) {
                runCatching {
                    val typedNode = addChatFileTypeUseCase(node, chatId, msgId)
                    getPreviewUseCase(typedNode)
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
}