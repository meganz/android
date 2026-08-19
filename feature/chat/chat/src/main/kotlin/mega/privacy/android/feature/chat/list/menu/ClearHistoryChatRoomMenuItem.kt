package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.feature.chat.list.model.ChatRoomMenuItemConfirmation
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Clears the whole message history of a chat, after confirmation.
 */
internal class ClearHistoryChatRoomMenuItem @Inject constructor(
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
) : ChatRoomMenuItem {

    override fun label(item: ChatRoomItem) = sharedR.string.title_properties_chat_clear
    override fun icon(item: ChatRoomItem) = IconPack.Medium.Thin.Outline.Eraser
    override val testTag = CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG

    override val confirmation = ChatRoomMenuItemConfirmation(
        title = sharedR.string.title_properties_chat_clear,
        message = sharedR.string.confirmation_clear_chat_history,
        confirmButtonText = sharedR.string.general_clear,
        testTag = CHAT_ROOM_ACTIONS_CLEAR_DIALOG_TAG,
    )

    override suspend fun shouldDisplay(item: ChatRoomItem) = !item.isArchived &&
            item.hasPermissions &&
            !(item is ChatRoomItem.NoteToSelfChatRoomItem && item.isEmptyNoteToSelfChatRoom)

    override suspend fun onClick(item: ChatRoomItem) {
        clearChatHistoryUseCase(item.chatId)
    }
}

internal const val CHAT_ROOM_ACTIONS_CLEAR_HISTORY_TAG = "chat_room_actions_sheet:clear_history"
internal const val CHAT_ROOM_ACTIONS_CLEAR_DIALOG_TAG = "chat_room_actions_sheet:clear_dialog"
