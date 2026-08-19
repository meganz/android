package mega.privacy.android.feature.chat.list.menu

import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Unarchives an archived chat.
 */
internal class UnarchiveChatRoomMenuItem @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
) : ChatRoomMenuItem {

    override fun label(item: ChatRoomItem) = sharedR.string.general_unarchive
    override fun icon(item: ChatRoomItem) = IconPack.Medium.Thin.Outline.ArchiveArrowUp
    override val testTag = CHAT_ROOM_ACTIONS_UNARCHIVE_TAG

    override suspend fun shouldDisplay(item: ChatRoomItem) = item.isArchived

    override suspend fun onClick(item: ChatRoomItem) {
        archiveChatUseCase(item.chatId, false)
    }
}

internal const val CHAT_ROOM_ACTIONS_UNARCHIVE_TAG = "chat_room_actions_sheet:unarchive"
