package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import javax.inject.Inject

/**
 * Monitors the pending messages that are ready to upload and start the upload to the chat folder and waits until the sdk scanning finishes
 *  It returns a flow that emits the number of pending messages in preparing state, with always a 0 when everything is done
 */
class StartUploadingAllPendingMessagesUseCase @Inject constructor(
    private val startChatUploadAndWaitScanningFinishedUseCase: StartChatUploadAndWaitScanningFinishedUseCase,
    private val monitorPendingMessagesByStateUseCase: MonitorPendingMessagesByStateUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
) {
    /**
     * Invoke
     *
     * @return a flow that emits the number of pending messages in READY_TO_UPLOAD state, with always a 0 when everything is done
     */
    operator fun invoke(): Flow<Int> {
        return monitorPendingMessagesByStateUseCase(PendingMessageState.READY_TO_UPLOAD)
            .conflate()
            .map { pendingMessageList ->
                pendingMessageList
                    .groupBy { fileSystemRepository.getFileByPath(it.filePath) to it.name }
                    .forEach { (filesAndNames, pendingMessages) ->
                        // Start the upload and wait until scanning has finished:
                        // - One by one because the pending messages are different
                        // - Wait until scanning finished as required by the sdk
                        filesAndNames.first?.let { file ->
                            startChatUploadAndWaitScanningFinishedUseCase(
                                mapOf(file to filesAndNames.second),
                                pendingMessages.map { it.id }
                            )
                            updatePendingMessageUseCase(
                                updatePendingMessageRequests = pendingMessages.map { pendingMessage ->
                                    UpdatePendingMessageStateRequest(
                                        pendingMessage.id,
                                        PendingMessageState.UPLOADING,
                                    )
                                }.toTypedArray()
                            )
                        } ?: run {

                            updatePendingMessageUseCase(
                                updatePendingMessageRequests = pendingMessages.map { pendingMessage ->
                                    UpdatePendingMessageStateRequest(
                                        pendingMessage.id,
                                        PendingMessageState.ERROR_UPLOADING,
                                    )
                                }.toTypedArray()
                            )
                        }
                    }
                pendingMessageList.size
            }
    }
}
