package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
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
) : AbstractAttachmentMessageViewModel<NodeAttachmentMessage>(fileSizeStringMapper) {
    override fun onMessageAdded(attachmentMessage: NodeAttachmentMessage, chatId: Long) {
        updatePreview(attachmentMessage, chatId)
    }

    private fun updatePreview(nodeAttachmentMessage: NodeAttachmentMessage, chatId: Long) {
        viewModelScope.launch {
            val node = nodeAttachmentMessage.fileNode
            val msgId = nodeAttachmentMessage.msgId
            if (node.hasPreview && getUiStateFlow(msgId).value.imageUri == null) {
                runCatching {
                    val typedNode = addChatFileTypeUseCase(node, chatId, msgId)
                    getPreviewUseCase(typedNode) //getPreview doesn't work with chat nodes form others, we need to get the node with GetChatFileUseCase and then update repository to get the preview from node
                }.onSuccess { previewFile ->
                    getUiStateFlow(msgId).update {
                        it.copy(
                            imageUri = previewFile?.toString(),
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