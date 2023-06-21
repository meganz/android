package mega.privacy.android.app.presentation.chat.list.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Chat tab enum representing each view tab
 *
 * @property titleStringRes     Tab title resource string
 */
enum class ChatTab(
    @StringRes val titleStringRes: Int,
) {
    /**
     * Chats
     */
    CHATS(R.string.chats_label),

    /**
     * Meetings
     */
    MEETINGS(R.string.chat_tab_meetings_title)
}
