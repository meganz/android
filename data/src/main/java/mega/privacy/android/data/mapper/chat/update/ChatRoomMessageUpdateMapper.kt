package mega.privacy.android.data.mapper.chat.update

import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.domain.entity.chat.room.update.ChatRoomMessageUpdate
import mega.privacy.android.domain.entity.chat.room.update.HistoryTruncatedByRetentionTime
import mega.privacy.android.domain.entity.chat.room.update.MessageLoaded
import mega.privacy.android.domain.entity.chat.room.update.MessageReceived
import mega.privacy.android.domain.entity.chat.room.update.MessageUpdate
import javax.inject.Inject

internal class ChatRoomMessageUpdateMapper @Inject constructor(
    private val chatMessageMapper: ChatMessageMapper,
) {

    suspend operator fun invoke(update: ChatRoomUpdate): ChatRoomMessageUpdate? =
        when (update) {
            is ChatRoomUpdate.OnHistoryTruncatedByRetentionTime -> HistoryTruncatedByRetentionTime(
                chatMessageMapper(update.msg)
            )

            is ChatRoomUpdate.OnMessageLoaded -> update.msg?.let {
                MessageLoaded(
                    chatMessageMapper(
                        it
                    )
                )
            }

            is ChatRoomUpdate.OnMessageReceived -> update.msg?.let {
                MessageReceived(
                    chatMessageMapper(it)
                )
            }

            is ChatRoomUpdate.OnMessageUpdate -> update.msg?.let {
                MessageUpdate(
                    chatMessageMapper(
                        it
                    )
                )
            }

            else -> null
        }
}