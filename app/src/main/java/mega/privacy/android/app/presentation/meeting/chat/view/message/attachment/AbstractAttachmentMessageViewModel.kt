package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.domain.entity.chat.messages.AttachmentMessage
import java.util.concurrent.ConcurrentHashMap

/**
 * Common view model for messages with attachments: pending messages and sent messages
 */
abstract class AbstractAttachmentMessageViewModel<T : AttachmentMessage>(
    private val fileSizeStringMapper: FileSizeStringMapper,
) : ViewModel() {


    /**
     * Ui state of all the handled messages
     * key: msgId, value: ui state flow
     */
    private val _uiStateFlowMap: ConcurrentHashMap<Long, MutableStateFlow<AttachmentMessageUiState>> =
        ConcurrentHashMap()

    /**
     * Get the [MutableStateFlow] for a given attachment message which has [msgId]
     */
    internal fun getUiStateFlow(msgId: Long): MutableStateFlow<AttachmentMessageUiState> =
        _uiStateFlowMap.getOrPut(msgId) {
            MutableStateFlow(AttachmentMessageUiState())
        }

    /**
     * Add attachment message, so view model can start handling it.
     */
    fun addAttachmentMessage(attachmentMessage: T, chatId: Long) {
        if (_uiStateFlowMap.contains(attachmentMessage.msgId)) {
            //already handled, we can skip this
            return
        }
        getUiStateFlow(attachmentMessage.msgId).update {
            it.copy(
                fileName = attachmentMessage.fileName,
                fileSize = fileSizeStringMapper(attachmentMessage.fileSize),
            )
        }
        onMessageAdded(attachmentMessage, chatId)
    }

    /**
     * Event to handle message added
     */
    abstract fun onMessageAdded(attachmentMessage: T, chatId: Long)


}