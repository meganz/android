package mega.privacy.android.app.presentation.meeting.chat.view.message.attachment

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.usecase.transfers.chatuploads.MonitorPendingMessageTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.MonitorPendingMessagesCompressionProgressUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import javax.inject.Inject

/**
 * View model for pending messages
 */
@HiltViewModel
class PendingAttachmentMessageViewModel @Inject constructor(
    private val monitorPendingMessageTransferEventsUseCase: MonitorPendingMessageTransferEventsUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val monitorPendingMessagesCompressionProgressUseCase: MonitorPendingMessagesCompressionProgressUseCase,
    fileSizeStringMapper: FileSizeStringMapper,
    durationInSecondsTextMapper: DurationInSecondsTextMapper,
    fileTypeIconMapper: FileTypeIconMapper,
) : AbstractAttachmentMessageViewModel<PendingAttachmentMessage>(
    fileSizeStringMapper,
    durationInSecondsTextMapper,
    fileTypeIconMapper
) {
    init {
        monitorUploads()
        monitorCompression()
        monitorPausedTransfers()
    }

    private fun monitorUploads() {
        viewModelScope.launch {
            monitorPendingMessageTransferEventsUseCase()
                .collectLatest { (pendingMessageIds, transfer) ->
                    val areTransfersPaused = areTransfersPausedUseCase()
                    pendingMessageIds.forEach { pendingMessageId ->
                        getUiStateFlow(pendingMessageId)?.update {
                            it.copy(
                                loadProgress = transfer.progress,
                                areTransfersPaused = areTransfersPaused
                            )
                        }
                    }
                }
        }
    }

    private fun monitorCompression() {
        viewModelScope.launch {
            monitorPendingMessagesCompressionProgressUseCase()
                .collect { idProgressMap ->
                    idProgressMap.forEach { (pendingMessageId, compressionProgress) ->
                        getUiStateFlow(pendingMessageId)?.update {
                            it.copy(
                                compressionProgress = compressionProgress,
                            )
                        }
                    }
                }
        }
    }

    private fun monitorPausedTransfers() {
        viewModelScope.launch {
            monitorPausedTransfersUseCase().collectLatest { areTransfersPaused ->
                updatePausedTransfers(areTransfersPaused)
            }
        }
    }

    override fun createFirstUiState(attachmentMessage: PendingAttachmentMessage): AttachmentMessageUiState {
        return super.createFirstUiState(attachmentMessage)
            .copy(
                previewUri = attachmentMessage.filePath,
            )
    }
}

