package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.pendingMessageId
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import javax.inject.Inject

/**
 * View model for pending messages
 */
@HiltViewModel
class PendingAttachmentMessageViewModel @Inject constructor(
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    fileSizeStringMapper: FileSizeStringMapper,
    durationInSecondsTextMapper: DurationInSecondsTextMapper,
    fileTypeIconMapper: FileTypeIconMapper
) : AbstractAttachmentMessageViewModel<PendingAttachmentMessage>(
    fileSizeStringMapper,
    durationInSecondsTextMapper,
    fileTypeIconMapper
) {
    init {
        monitorUploads()
    }

    private fun monitorUploads() {
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .filterIsInstance<TransferEvent.TransferUpdateEvent>()
                .mapNotNull { event ->
                    event.transfer.pendingMessageId()
                        ?.let { it to event.transfer }
                }
                .collectLatest { (pendingMessageId, transfer) ->
                    getUiStateFlow(pendingMessageId)?.update {
                        it.copy(loadProgress = transfer.progress)
                    }
                }
        }
    }

    override fun createFirstUiState(attachmentMessage: PendingAttachmentMessage): AttachmentMessageUiState {
        return super.createFirstUiState(attachmentMessage)
            .copy(
                previewUri = attachmentMessage.file?.absolutePath,
            )
    }
}

