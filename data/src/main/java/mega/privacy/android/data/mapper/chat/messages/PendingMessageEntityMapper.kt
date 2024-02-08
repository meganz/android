package mega.privacy.android.data.mapper.chat.messages

import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import javax.inject.Inject

/**
 * Pending message entity mapper
 */
class PendingMessageEntityMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param savePendingMessageRequest
     * @return PendingMessageEntity
     */
    operator fun invoke(
        savePendingMessageRequest: SavePendingMessageRequest,
    ): PendingMessageEntity {
        return PendingMessageEntity(
            chatId = savePendingMessageRequest.chatId,
            type = savePendingMessageRequest.type,
            uploadTimestamp = savePendingMessageRequest.uploadTimestamp,
            state = savePendingMessageRequest.state,
            tempIdKarere = savePendingMessageRequest.tempIdKarere,
            videoDownSampled = savePendingMessageRequest.videoDownSampled,
            filePath = savePendingMessageRequest.filePath,
            nodeHandle = savePendingMessageRequest.nodeHandle,
            fingerprint = savePendingMessageRequest.fingerprint,
            name = savePendingMessageRequest.name,
            transferTag = savePendingMessageRequest.transferTag
        )
    }

}