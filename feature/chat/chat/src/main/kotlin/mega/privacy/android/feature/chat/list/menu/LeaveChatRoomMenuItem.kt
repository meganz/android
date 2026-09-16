package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Leaves the picked active group chats, after confirmation.
 */
internal class LeaveChatRoomMenuItem @Inject constructor(
    private val leaveChatUseCase: LeaveChatUseCase,
) : ChatRoomMenuItem {

    override fun label(items: List<ChatRoomItem>) = sharedR.string.general_leave
    override fun icon(items: List<ChatRoomItem>) = IconPack.Medium.Thin.Outline.LogOut02
    override val testTag = CHAT_ROOM_ACTIONS_LEAVE_TAG
    override val isDestructive = true

    override val confirmation = ChatRoomMenuItemConfirmation(
        title = sharedR.string.title_confirmation_leave_group_chat,
        message = sharedR.string.confirmation_leave_group_chat,
        confirmButtonText = sharedR.string.general_leave,
        testTag = CHAT_ROOM_ACTIONS_LEAVE_DIALOG_TAG,
    )

    override suspend fun shouldDisplay(item: ChatRoomItem) =
        !item.isArchived && item is ChatRoomItem.GroupChatRoomItem && item.isActive

    override suspend fun onClick(items: List<ChatRoomItem>) =
        items.failuresOf { leaveChatUseCase(it.chatId) }

    override fun failureMessage(items: List<ChatRoomItem>) =
        ChatRoomActionMessage.Text(
            messageId = sharedR.string.general_request_failed_message,
            args = emptyList(),
        )
}

internal const val CHAT_ROOM_ACTIONS_LEAVE_TAG = "chat_room_actions_sheet:leave"
internal const val CHAT_ROOM_ACTIONS_LEAVE_DIALOG_TAG = "chat_room_actions_sheet:leave_dialog"
