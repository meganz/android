package mega.privacy.android.data.mapper.chat.messages

import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.uri.UriPath
import javax.inject.Inject

/**
 * Pending message mapper
 */
class PendingMessageMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param entity
     * @return
     */
    operator fun invoke(entity: PendingMessageEntity): PendingMessage {
        return with(entity) {
            PendingMessage(
                id = pendingMessageId ?: -1,
                transferUniqueId = transferUniqueId ?: -1,
                chatId = chatId,
                type = type,
                uploadTimestamp = uploadTimestamp,
                state = state.value,
                tempIdKarere = tempIdKarere,
                videoDownSampled = videoDownSampled,
                uriPath = UriPath(filePath),
                nodeHandle = nodeHandle,
                fingerprint = fingerprint,
                name = name,
                originalUriPath = UriPath(originalUriPath),
            )
        }
    }
}