package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.file.GetFileFromUriUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWithWorkerUseCase
import javax.inject.Inject

/**
 * Begin uploading files to later attach them to a chat. It also generates a pending message to represent this attachment in the chat while it's being uploaded.
 */
class SendChatAttachmentsUseCase @Inject constructor(
    private val startChatUploadsWithWorkerUseCase: StartChatUploadsWithWorkerUseCase,
    private val getFileFromUriUseCase: GetFileFromUriUseCase,
    private val chatMessageRepository: ChatMessageRepository,
    private val deviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase
) {
    /**
     * Invoke
     *
     * @param chatId the id of the chat where these files will be attached
     * @param uris String representation of the files,
     */
    operator fun invoke(chatId: Long, uris: List<String>) =
        flow {
            emitAll(
                //each file is sent as a single message in parallel
                uris
                    .mapNotNull { uriString ->
                        getFileFromUriUseCase(uriString, CHAT_TEMPORARY_FOLDER)
                    }
                    .map { file ->
                        val pendingMessageId = chatMessageRepository.savePendingMessage(
                            SavePendingMessageRequest(
                                chatId = chatId,
                                type = -1,
                                uploadTimestamp = deviceCurrentTimeUseCase() / 1000,
                                state = PendingMessageState.UPLOADING,
                                tempIdKarere = -1,
                                videoDownSampled = null,
                                filePath = file.path,
                                nodeHandle = -1,
                                fingerprint = null,
                                name = file.name,
                                transferTag = -1,
                            )
                        ).id
                        startChatUploadsWithWorkerUseCase(listOf(file), pendingMessageId)
                    }.merge()
            )
        }
}

private const val CHAT_TEMPORARY_FOLDER = "chatTempMEGA"