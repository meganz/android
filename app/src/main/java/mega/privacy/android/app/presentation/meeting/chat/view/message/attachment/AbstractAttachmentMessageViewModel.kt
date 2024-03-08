package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.node.model.mapper.getFileIconChat
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.chat.messages.AttachmentMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * Common view model for messages with attachments: pending messages and sent messages
 */
abstract class AbstractAttachmentMessageViewModel<T : AttachmentMessage>(
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : ViewModel() {


    /**
     * Ui state of all the handled messages
     * key: msgId, value: ui state flow
     */
    private val _uiStateFlowMap: ConcurrentHashMap<Long, MutableStateFlow<AttachmentMessageUiState>> =
        ConcurrentHashMap()

    /**
     * Get or create the [MutableStateFlow] for a given attachment message and chat id
     */
    internal fun updateAndGetUiStateFlow(
        attachmentMessage: T,
    ): MutableStateFlow<AttachmentMessageUiState> {
        if (_uiStateFlowMap.containsKey(attachmentMessage.msgId)) {
            updateMessage(attachmentMessage)
        }
        return _uiStateFlowMap.getOrPut(attachmentMessage.msgId) {
            MutableStateFlow(
                createFirstUiState(attachmentMessage)
            ).also {
                onMessageAdded(it, attachmentMessage)
            }
        }
    }

    private fun updateMessage(attachmentMessage: T) {
        _uiStateFlowMap[attachmentMessage.msgId]?.update {
            it.copy(isError = attachmentMessage.isSendError())
        }
    }

    internal fun getUiStateFlow(messageId: Long): MutableStateFlow<AttachmentMessageUiState>? =
        _uiStateFlowMap[messageId]

    /**
     * Event to handle message added
     */
    protected open fun onMessageAdded(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        attachmentMessage: T,
    ) = Unit

    /**
     * Creates the initial ui state for this attachment message
     */
    internal open fun createFirstUiState(attachmentMessage: T) = AttachmentMessageUiState(
        fileName = attachmentMessage.fileName,
        fileSize = fileSizeStringMapper(attachmentMessage.fileSize),
        duration = attachmentMessage.duration?.let { durationInSecondsTextMapper(it) },
        fileTypeResId = getFileIconChat(attachmentMessage.fileType),
        isError = attachmentMessage.isSendError()
    )
}