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
 * @property header Section header to render above this row on the meetings list, null when the row
 * does not start a new section.
 * @property pendingMeeting Scheduled-meeting decoration for a pending meeting row, null for regular
 * chats, non-pending meetings and note-to-self.
 * @property showRecurringIcon Whether to show the recurring-meeting indicator next to the title.
 * @property unreadCount Number of unread messages.
 * @property isMuted Whether notifications are muted for the chat room.
 * @property highlight Whether the last message should be highlighted.
 * @property isNoteToSelf Whether the chat room is the note-to-self chat.
 * @property avatar Leading avatar to render for the row.
 * @property status Online-status indicator for the peer; [ContactItemStatus.Unknown] hides it.
 * @property hasOngoingCall Whether the chat room currently has a call in progress.
 */
@Immutable
data class ChatRoomUiItem(
    val chatId: Long,
    val title: String,
    val lastMessage: String?,
    val lastTimestampFormatted: String?,
    val header: String?,
    val pendingMeeting: PendingMeeting?,
    val showRecurringIcon: Boolean,
    val unreadCount: Int,
    val isMuted: Boolean,
    val highlight: Boolean,
    val isNoteToSelf: Boolean,
    val avatar: ChatRoomUiAvatar,
    val status: ContactItemStatus,
    val hasOngoingCall: Boolean,
) {

    /**
     * Scheduled-meeting decoration for a pending meeting row.
     */
    @Immutable
    sealed interface PendingMeeting {

        /**
         * Formatted scheduled time range of the meeting, e.g. "10:00am - 11:00am"; null while still
         * resolving.
         */
        val scheduledTime: String?

        /**
         * A one-off meeting scheduled once in the future.
         *
         * @property scheduledTime Formatted scheduled time range.
         */
        @Immutable
        data class OneOff(
            override val scheduledTime: String?,
        ) : PendingMeeting

        /**
         * A meeting that repeats on a schedule.
         *
         * @property scheduledTime Formatted scheduled time range.
         * @property recurrence How often the meeting repeats.
         */
        @Immutable
        data class Recurring(
            override val scheduledTime: String?,
            val recurrence: Recurrence,
        ) : PendingMeeting

        /**
         * Recurrence period of a recurring meeting.
         */
        enum class Recurrence {
            Daily,
            Weekly,
            Monthly,
        }
    }

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
