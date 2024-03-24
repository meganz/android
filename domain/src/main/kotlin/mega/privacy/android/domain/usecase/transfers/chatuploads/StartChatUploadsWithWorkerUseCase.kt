package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.chat.FoldersNotAllowedAsChatUploadException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Start uploading a list of files and folders to the chat uploads folder with the corresponding pending message id in their app data
 * and returns a Flow to monitor the progress until the nodes are scanned.
 * While the returned flow is not completed the app should be blocked to avoid other interaction with the sdk to avoid issues
 * Once the flow is completed the sdk will keep uploading and a ChatUploadsWorker will monitor updates globally.
 * If cancelled before completion the processing of the nodes will be cancelled
 */
class StartChatUploadsWithWorkerUseCase @Inject constructor(
    private val uploadFilesUseCase: UploadFilesUseCase,
    private val startChatUploadsWorkerUseCase: StartChatUploadsWorkerUseCase,
    private val isChatUploadsWorkerStartedUseCase: IsChatUploadsWorkerStartedUseCase,
    private val compressFileForChatUseCase: CompressFileForChatUseCase,
    private val updatePendingMessageUseCase: UpdatePendingMessageUseCase,
    private val chatMessageRepository: ChatMessageRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val attachNodeWithPendingMessageUseCase: AttachNodeWithPendingMessageUseCase,
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) : AbstractStartTransfersWithWorkerUseCase(cancelCancelTokenUseCase) {

    /**
     * Invoke
     *
     * @param file the file that will be uploaded to chats folder in the cloud drive. If it's a folder will be filtered out because folders are not allowed as chat uploads
     * @param pendingMessageId the message id to be included in the app data, so the ChatUploadsWorker can associate the files to the corresponding message
     * @param chatFilesFolderId the id of the folder where the files will be uploaded
     */
    operator fun invoke(
        file: File,
        pendingMessageId: Long,
        chatFilesFolderId: NodeId,
    ): Flow<MultiTransferEvent> = flow {
        if (!fileSystemRepository.isFilePath(file.path)) {
            emit(
                MultiTransferEvent.TransferNotStarted(
                    file,
                    FoldersNotAllowedAsChatUploadException()
                )
            )
            return@flow
        }
        val filesAndNames = mapOf(
            (runCatching { compressFileForChatUseCase(file) }.getOrNull() ?: file)
                    to chatMessageRepository.getPendingMessage(pendingMessageId)?.name
        )
        coroutineContext.ensureActive()
        val appData = TransferAppData.ChatUpload(pendingMessageId)
        startTransfersAndWorker(
            doTransfers = {
                uploadFilesUseCase(
                    filesAndNames, chatFilesFolderId, appData, false
                ).onEach { event ->
                    //update transfer tag on Start event
                    ((event as? MultiTransferEvent.SingleTransferEvent)?.transferEvent as? TransferEvent.TransferStartEvent)?.transfer?.tag?.let { transferTag ->
                        updatePendingMessageUseCase(
                            UpdatePendingMessageTransferTagRequest(pendingMessageId, transferTag)
                        )
                    }
                    //attach it if it's already uploaded
                    (event as? MultiTransferEvent.SingleTransferEvent)
                        ?.alreadyTransferredIds
                        ?.singleOrNull()
                        ?.takeIf { it.longValue != -1L }
                        ?.let { alreadyTransferredNodeId ->
                            attachNodeWithPendingMessageUseCase(
                                pendingMessageId,
                                alreadyTransferredNodeId
                            )
                        }
                }
            },
            startWorker = {
                startChatUploadsWorkerUseCase()
                //ensure worker has started and is listening to global events so we can finish uploadFilesUseCase
                isChatUploadsWorkerStartedUseCase()
            }
        )
    }
}