package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Toggles a chat's notifications: mutes an unmuted chat until the user turns them back on, or
 * unmutes a currently muted one. The presented label, icon and action all follow the chat's
 * current mute state.
 */
internal class MuteChatRoomMenuItem @Inject constructor(
    private val muteChatNotificationForChatRoomsUseCase: MuteChatNotificationForChatRoomsUseCase,
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase,
) : ChatRoomMenuItem {

    override fun label(item: ChatRoomItem) =
        if (item.isMuted) sharedR.string.general_unmute else sharedR.string.general_mute

    override fun icon(item: ChatRoomItem) =
        if (item.isMuted) IconPack.Medium.Thin.Outline.Bell else IconPack.Medium.Thin.Outline.BellOff

    override val testTag = CHAT_ROOM_ACTIONS_MUTE_TAG

    override suspend fun shouldDisplay(item: ChatRoomItem) =
        !item.isArchived && item.canToggleMute()

    override suspend fun onClick(item: ChatRoomItem) {
        if (item.isMuted) {
            unmuteChatNotificationUseCase(item.chatId)
        } else {
            muteChatNotificationForChatRoomsUseCase(
                chatIdList = listOf(item.chatId),
                muteOption = ChatPushNotificationMuteOption.MuteUntilTurnBackOn,
            )
        }
    }

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
