package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.isFinishScanningEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.GetPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetChatPitagTargetUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFileUseCase
import javax.inject.Inject

/**
 * Start uploading files with the related chat data and waits until all the scanning of the started transfers have finished (the SDK has stored all the related files)
 * this is usually a quick step, but it can take some time if the sdk is blocked doing other things
 * The use case also handles the transfer events to update the pending messages
 */
class StartChatUploadAndWaitScanningFinishedUseCase @Inject constructor(
    private val uploadFileUseCase: UploadFileUseCase,
    private val getOrCreateMyChatsFilesFolderIdUseCase: GetOrCreateMyChatsFilesFolderIdUseCase,
    private val getPendingMessageUseCase: GetPendingMessageUseCase,
    private val getChatPitagTargetUseCase: GetChatPitagTargetUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(
        uriPath: UriPath,
        fileName: String?,
        extraAppData: TransferAppData? = null,
        pitagTrigger: PitagTrigger,
        pendingMessageIds: List<Long>,
    ) {
        val appData = (pendingMessageIds.map {
            TransferAppData.ChatUpload(it)
        } + extraAppData).filterNotNull()
        val pitagTarget = pendingMessageIds
            .mapNotNull { getPendingMessageUseCase(it) }.let { pendingMessages ->
                getChatPitagTargetUseCase(pendingMessages)
            }

        uploadFileUseCase(
            uriPath = uriPath,
            fileName = fileName,
            appData = appData,
            getOrCreateMyChatsFilesFolderIdUseCase(),
            false,
            pitagTrigger = pitagTrigger,
            pitagTarget = pitagTarget
        ).firstOrNull { event ->
            event.isFinishScanningEvent
        }
    }
}