package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import javax.inject.Inject

/**
 * Monitors the pending messages that are added by the user to a chat and if it's not accessible by the sdk (it's a content Uri) copies the file to the cache folder and then:
 *  - if it needs compression: updates the pending message state to be compressed
 *  - if doesn't need compression: updates the pending message state to be ready to upload
 *  It returns a flow that emits the number of pending messages in preparing state
 */
class PrepareAllPendingMessagesUseCase @Inject constructor(
    private val monitorPendingMessagesByStateUseCase: MonitorPendingMessagesByStateUseCase,
    private val getFileForUploadUseCase: GetFileForUploadUseCase,
    private val chatAttachmentNeedsCompressionUseCase: ChatAttachmentNeedsCompressionUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
) {
    /**
     * Invoke
     *
     * @return a flow that emits the number of pending messages in preparing state, with always a 0 when everything is done
     */
    operator fun invoke(): Flow<Int> {
        return channelFlow {
            monitorPendingMessagesByStateUseCase(PendingMessageState.PREPARING)
                .conflate()
                .collect { pendingMessageList ->
                    send(pendingMessageList.size)
                    val semaphore = Semaphore(5) // to limit the parallel copy to cache
                    pendingMessageList
                        .groupBy { it.filePath }
                        .map { (uri, pendingMessages) ->
                            launch {
                                semaphore.withPermit {
                                    val pendingMessageIds = pendingMessages.map { it.id }
                                    val cacheCopy = getFileForUploadUseCase(
                                        uriOrPathString = uri,
                                        isChatUpload = true
                                    )
                                    when {
                                        cacheCopy == null -> {
                                            updatePendingMessageUseCase(
                                                updatePendingMessageRequests = pendingMessageIds.map { pendingMessageId ->
                                                    UpdatePendingMessageStateRequest(
                                                        pendingMessageId,
                                                        PendingMessageState.ERROR_UPLOADING,
                                                    )
                                                }.toTypedArray()
                                            )
                                        }

                                        chatAttachmentNeedsCompressionUseCase(cacheCopy) -> {
                                            updatePendingMessageUseCase(
                                                updatePendingMessageRequests = pendingMessageIds.map { pendingMessageId ->
                                                    UpdatePendingMessageStateAndPathRequest(
                                                        pendingMessageId,
                                                        PendingMessageState.COMPRESSING,
                                                        cacheCopy.path,
                                                    )
                                                }.toTypedArray()
                                            )
                                        }

                                        else -> {
                                            //ready to upload
                                            updatePendingMessageUseCase(
                                                updatePendingMessageRequests = pendingMessageIds.map { pendingMessageId ->
                                                    UpdatePendingMessageStateAndPathRequest(
                                                        pendingMessageId,
                                                        PendingMessageState.READY_TO_UPLOAD,
                                                        cacheCopy.path,
                                                    )
                                                }.toTypedArray()
                                            )
                                        }
                                    }
                                }
                            }
                        }.joinAll()
                }
        }
    }
}
