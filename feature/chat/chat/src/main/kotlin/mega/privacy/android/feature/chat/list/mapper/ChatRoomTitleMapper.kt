package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Resolves the user-facing title of a chat room for messages shown outside the list, where the
 * note-to-self chat is named by its localised label rather than by the title the SDK reports.
 */
internal class ChatRoomTitleMapper @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * @param item Domain chat room to name.
     * @return Title to show the user for [item].
     */
    operator fun invoke(item: ChatRoomItem): String = when (item) {
        is ChatRoomItem.NoteToSelfChatRoomItem ->
            context.getString(sharedR.string.chat_note_to_self_chat_title)

        else -> item.title
    }
}
