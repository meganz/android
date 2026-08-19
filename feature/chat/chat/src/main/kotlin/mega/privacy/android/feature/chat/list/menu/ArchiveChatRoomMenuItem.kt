package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Archives a chat.
 */
internal class ArchiveChatRoomMenuItem @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
) : ChatRoomMenuItem {

    override fun label(item: ChatRoomItem) = sharedR.string.general_archive
    override fun icon(item: ChatRoomItem) = IconPack.Medium.Thin.Outline.Archive
    override val testTag = CHAT_ROOM_ACTIONS_ARCHIVE_TAG

    override suspend fun shouldDisplay(item: ChatRoomItem) = !item.isArchived

    override suspend fun onClick(item: ChatRoomItem) {
        archiveChatUseCase(item.chatId, true)
    }
}

internal const val CHAT_ROOM_ACTIONS_ARCHIVE_TAG = "chat_room_actions_sheet:archive"
