package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import java.io.File
import javax.inject.Inject

/**
 * Start uploading files with the related chat data and waits until all the scanning of the started transfers have finished (the SDK has stored all the related files)
 * this is usually a quick step, but it can take some time if the sdk is blocked doing other things
 * The use case also handles the transfer events to update the pending messages
 */
class StartChatUploadAndWaitScanningFinishedUseCase @Inject constructor(
    private val uploadFilesUseCase: UploadFilesUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
    private val handleChatUploadTransferEventUseCase: HandleChatUploadTransferEventUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(
        filesAndNames: Map<File, String?>,
        pendingMessageIds: List<Long>,
    ) {
        uploadFilesUseCase(
            filesAndNames,
            getMyChatsFilesFolderIdUseCase(),
            pendingMessageIds.map {
                TransferAppData.ChatUpload(it)
            },
            false
        ).onEach { event ->
            handleChatUploadTransferEventUseCase(
                event = event,
                pendingMessageIds = pendingMessageIds.toLongArray()
            )
        }.firstOrNull { event ->
            (event as? MultiTransferEvent.SingleTransferEvent)?.scanningFinished == true
        }
    }
}