package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.file.DoesCacheHaveSufficientSpaceForUrisUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.StartChatUploadsWorkerUseCase
import javax.inject.Inject

/**
 * Begin uploading files to later attach them to a chat. It also generates a pending message to represent this attachment in the chat while it's being uploaded.
 */
class SendChatAttachmentsUseCase @Inject constructor(
    private val startChatUploadsWorkerUseCase: StartChatUploadsWorkerUseCase,
    private val chatMessageRepository: ChatMessageRepository,
    private val deviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
    private val doesCacheHaveSufficientSpaceForUrisUseCase: DoesCacheHaveSufficientSpaceForUrisUseCase,
) {
    /**
     * Invoke
     *
     * @param chatIds the id of the chat where these files will be attached
     * @param urisWithNames String representation of the files associated with the desired node's name or null if there are no changes
     * @param isVoiceClip
     */
    suspend operator fun invoke(
        urisWithNames: Map<String, String?>,
        isVoiceClip: Boolean = false,
        vararg chatIds: Long,
    ) {
        if (!doesCacheHaveSufficientSpaceForUrisUseCase(urisWithNames.keys.toList())) {
            throw NotEnoughStorageException()
        }
        urisWithNames.forEach { (uri, name) ->
            chatMessageRepository.savePendingMessages(
                SavePendingMessageRequest(
                    chatId = chatIds.first(),
                    type = if (isVoiceClip) PendingMessage.TYPE_VOICE_CLIP else -1,
                    uploadTimestamp = deviceCurrentTimeUseCase() / 1000,
                    state = PendingMessageState.PREPARING,
                    tempIdKarere = -1,
                    videoDownSampled = null,
                    filePath = uri,
                    nodeHandle = -1,
                    fingerprint = null,
                    name = name,
                    transferTag = -1,
                ),
                chatIds.asList()
            ).forEach { pendingMessageId ->
                chatMessageRepository.cacheOriginalPathForPendingMessage(pendingMessageId, uri)
            }
        }
        startChatUploadsWorkerUseCase()
    }
}