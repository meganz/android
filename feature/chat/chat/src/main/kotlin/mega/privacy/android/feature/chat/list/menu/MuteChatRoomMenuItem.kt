package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Turns notifications for the picked chats off, or back on.
 *
 * The direction is decided for the selection as a whole: it unmutes only when every picked chat is
 * already muted, and mutes otherwise. A selection mixing muted and unmuted chats therefore reads
 * "Mute" and mutes all of them, so the label always describes what actually happens. The legacy
 * selection toolbar offered "Mute" only when nothing in the selection was muted and "Unmute" only
 * when everything was, leaving a mixed selection with neither; muting is the direction that agrees
 * with both of those cases and never turns notifications back on behind the user's back.
 */
internal class MuteChatRoomMenuItem @Inject constructor(
    private val muteChatNotificationForChatRoomsUseCase: MuteChatNotificationForChatRoomsUseCase,
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase,
) : ChatRoomMenuItem {

    override fun label(items: List<ChatRoomItem>) =
        if (items.unmutes()) sharedR.string.general_unmute else sharedR.string.general_mute

    override fun icon(items: List<ChatRoomItem>) =
        if (items.unmutes()) {
            IconPack.Medium.Thin.Outline.Bell
        } else {
            IconPack.Medium.Thin.Outline.BellOff
        }

    override val testTag = CHAT_ROOM_ACTIONS_MUTE_TAG

    override suspend fun shouldDisplay(item: ChatRoomItem) =
        !item.isArchived && item.canToggleMute()

    override suspend fun onClick(items: List<ChatRoomItem>): List<ChatRoomItem> {
        if (items.unmutes()) {
            return items.failuresOf { unmuteChatNotificationUseCase(it.chatId) }
        }
        muteChatNotificationForChatRoomsUseCase(
            chatIdList = items.map(ChatRoomItem::chatId),
            muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
        )
        return emptyList()
    }

    override fun failureMessage(items: List<ChatRoomItem>) =
        ChatRoomActionMessage.Text(
            messageId = sharedR.string.general_request_failed_message,
            args = emptyList(),
        )

    private fun List<ChatRoomItem>.unmutes(): Boolean = isNotEmpty() && all { it.isMuted }

    /**
     * Mirrors the legacy availability rule for muting: note-to-self chats can never be muted,
     * groups can be muted while active, and other chats require write permissions.
     */
    private fun ChatRoomItem.canToggleMute(): Boolean = when (this) {
        is ChatRoomItem.NoteToSelfChatRoomItem -> false
        is ChatRoomItem.GroupChatRoomItem -> isActive
        else -> hasPermissions
    }
}

internal const val CHAT_ROOM_ACTIONS_MUTE_TAG = "chat_room_actions_sheet:mute"
