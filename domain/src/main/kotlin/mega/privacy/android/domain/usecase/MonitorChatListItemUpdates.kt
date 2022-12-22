package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.chat.ChatListItem

/**
 * Use case for monitoring updates on chat list item statuses.
 */
fun interface MonitorChatListItemUpdates {

    /**
     * Invoke.
     *
     * @return Flow of [ChatListItem].
     */
    operator fun invoke(): Flow<ChatListItem>
}