package mega.privacy.android.feature.chat.list.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.components.contact.state.ContactItemStatus

/**
 * UI model for one row in the chat list.
 *
 * @property chatId Chat id of the chat room.
 * @property title Title of the chat room.
 * @property lastMessage Last message preview, null when there is nothing to show.
 * @property lastTimestampFormatted Formatted last activity time, null while still resolving.
 * @property scheduledTimestampFormatted Formatted scheduled time for pending meetings.
 * @property unreadCount Number of unread messages.
 * @property isMuted Whether notifications are muted for the chat room.
 * @property highlight Whether the last message should be highlighted.
 * @property isNoteToSelf Whether the chat room is the note-to-self chat.
 * @property avatar Leading avatar to render for the row.
 * @property status Online-status indicator for the peer; [ContactItemStatus.Unknown] hides it.
 */
@Immutable
data class ChatRoomUiItem(
    val chatId: Long,
    val title: String,
    val lastMessage: String?,
    val lastTimestampFormatted: String?,
    val scheduledTimestampFormatted: String?,
    val unreadCount: Int,
    val isMuted: Boolean,
    val highlight: Boolean,
    val isNoteToSelf: Boolean,
    val avatar: ChatRoomUiAvatar,
    val status: ContactItemStatus,
) {

    /**
     * Leading avatar variants for a chat list row.
     */
    @Immutable
    sealed interface ChatRoomUiAvatar {

        /**
         * Avatar of a single peer.
         *
         * @property placeholderText Text placeholder shown when there is no picture.
         * @property filePath Absolute path of the avatar picture file, if any.
         * @property color Avatar background color, if any.
         */
        @Immutable
        data class Peer(
            val placeholderText: String?,
            val filePath: String?,
            val color: Int?,
        ) : ChatRoomUiAvatar

        /**
         * Icon avatar for group chat rooms.
         */
        data object Group : ChatRoomUiAvatar

        /**
         * Icon avatar for meeting chat rooms.
         */
        data object Meeting : ChatRoomUiAvatar

        /**
         * Icon avatar for the note-to-self chat room.
         */
        data object NoteToSelf : ChatRoomUiAvatar
    }
}
