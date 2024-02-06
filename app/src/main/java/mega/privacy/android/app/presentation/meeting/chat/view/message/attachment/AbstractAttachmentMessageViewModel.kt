package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    internal fun getOrPutUiStateFlow(
        attachmentMessage: T,
        chatId: Long,
    ): MutableStateFlow<AttachmentMessageUiState> =
        _uiStateFlowMap.getOrPut(attachmentMessage.msgId) {
            MutableStateFlow(
                AttachmentMessageUiState(
                    fileName = attachmentMessage.fileName,
                    fileSize = fileSizeStringMapper(attachmentMessage.fileSize),
                    duration = durationInSecondsTextMapper(attachmentMessage.duration),
                    fileTypeResId = getFileIconChat(attachmentMessage.fileType),
                )
            ).also {
                onMessageAdded(it, attachmentMessage, chatId)
            }
        }

    /**
     * Event to handle message added
     */
    abstract fun onMessageAdded(
        mutableStateFlow: MutableStateFlow<AttachmentMessageUiState>,
        attachmentMessage: T,
        chatId: Long,
    )
}