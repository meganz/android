package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.chat.FoldersNotAllowedAsChatUploadException
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractStartTransfersWithWorkerUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import java.io.File
import javax.inject.Inject

/**
 * Start uploading a list of files and folders to the chat uploads folder with the corresponding pending message id in their app data
 * and returns a Flow to monitor the progress until the nodes are scanned.
 * While the returned flow is not completed the app should be blocked to avoid other interaction with the sdk to avoid issues
 * Once the flow is completed the sdk will keep uploading and a ChatUploadsWorker will monitor updates globally.
 * If cancelled before completion the processing of the nodes will be cancelled
 */
class StartChatUploadsWithWorkerUseCase @Inject constructor(
    private val uploadFilesUseCase: UploadFilesUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
) : AbstractStartTransfersWithWorkerUseCase(cancelCancelTokenUseCase) {

    /**
     * Invoke
     *
     * @param files a list of files that will be uploaded to chats folder in the cloud drive. Any folder will be filtered out because folders are not allowed as chat uploads
     * @param pendingMessageId the message id to be included in the app data, so the ChatUploadsWorker can associate the files to the corresponding message
     */
    operator fun invoke(
        files: List<File>,
        pendingMessageId: Long,
    ): Flow<MultiTransferEvent> = flow {
        val onlyFiles = files.filter {
            if (it.isFile) {
                true
            } else {
                emit(
                    MultiTransferEvent.TransferNotStarted(
                        it,
                        FoldersNotAllowedAsChatUploadException()
                    )
                )
                false
            }
        }
        //images and videos should be compressed, will be done in AND-17948
        val chatFilesFolderId = getMyChatsFilesFolderIdUseCase()
        val appData = TransferAppData.ChatUpload(pendingMessageId)
        startTransfersAndWorker(
            doTransfers = {
                uploadFilesUseCase(
                    onlyFiles, chatFilesFolderId, appData, false
                )
            },
            startWorker = {
                //this will be implemented in AND-17904
            }
        )
    }
}