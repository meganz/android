package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetFileForChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import javax.inject.Inject

/**
 * Begin uploading files to later attach them to a chat. It also generates a pending message to represent this attachment in the chat while it's being uploaded.
 */
class SendChatAttachmentsUseCase @Inject constructor(
    private val startChatUploadsWithWorkerUseCase: StartChatUploadsWithWorkerUseCase,
    private val getFileForChatUploadUseCase: GetFileForChatUploadUseCase,
    private val chatMessageRepository: ChatMessageRepository,
    private val deviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
    private val getMyChatsFilesFolderIdUseCase: GetMyChatsFilesFolderIdUseCase,
) {
    /**
     * Invoke
     *
     * @param chatId the id of the chat where these files will be attached
     * @param urisWithNames String representation of the files associated with the desired node's name or null if there are no changes
     * @param isVoiceClip
     */
    operator fun invoke(
        chatId: Long,
        urisWithNames: Map<String, String?>,
        isVoiceClip: Boolean = false,
    ) =
        flow {
            val chatFolderId = getMyChatsFilesFolderIdUseCase()
            emitAll(
                //each file is sent as a single message in parallel
                urisWithNames.mapKeys {
                    getFileForChatUploadUseCase(it.key)
                }.mapNotNull { (file, name) ->
                    file?.let {
                        val pendingMessageId = chatMessageRepository.savePendingMessage(
                            SavePendingMessageRequest(
                                chatId = chatId,
                                type = if (isVoiceClip) PendingMessage.TYPE_VOICE_CLIP else -1,
                                uploadTimestamp = deviceCurrentTimeUseCase() / 1000,
                                state = PendingMessageState.UPLOADING,
                                tempIdKarere = -1,
                                videoDownSampled = null,
                                filePath = file.path,
                                nodeHandle = -1,
                                fingerprint = null,
                                name = name,
                                transferTag = -1,
                            )
                        ).id
                        startChatUploadsWithWorkerUseCase(file, pendingMessageId, chatFolderId)
                    }
                }.merge()
            )
        }
}