package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.feature.chat.list.mapper.ChatRoomTitleMapper
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Archives the picked chats.
 */
internal class ArchiveChatRoomMenuItem @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val chatRoomTitleMapper: ChatRoomTitleMapper,
) : ChatRoomMenuItem {

    override fun label(items: List<ChatRoomItem>) = sharedR.string.general_archive
    override fun icon(items: List<ChatRoomItem>) = IconPack.Medium.Thin.Outline.Archive
    override val testTag = CHAT_ROOM_ACTIONS_ARCHIVE_TAG

    override suspend fun shouldDisplay(item: ChatRoomItem) = !item.isArchived

    override suspend fun onClick(items: List<ChatRoomItem>) =
        items.failuresOf { archiveChatUseCase(it.chatId, true) }

    override fun successMessage(items: List<ChatRoomItem>) = items.singleOrNull()
        ?.let {
            ChatRoomActionMessage.Text(
                messageId = sharedR.string.success_archive_chat,
                args = listOf(chatRoomTitleMapper(it)),
            )
        }
        ?: ChatRoomActionMessage.Plural(
            messageId = sharedR.plurals.chat_list_chats_archived_message,
            quantity = items.size,
        )

    override fun failureMessage(items: List<ChatRoomItem>) = items.singleOrNull()
        ?.let {
            ChatRoomActionMessage.Text(
                messageId = sharedR.string.error_archive_chat,
                args = listOf(chatRoomTitleMapper(it)),
            )
        }
        ?: ChatRoomActionMessage.Text(
            messageId = sharedR.string.general_request_failed_message,
            args = emptyList(),
        )
}

internal const val CHAT_ROOM_ACTIONS_ARCHIVE_TAG = "chat_room_actions_sheet:archive"
