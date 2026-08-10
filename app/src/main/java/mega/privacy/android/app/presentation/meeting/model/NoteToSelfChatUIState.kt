package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * UI state for note to self chat
 *
 * @property isNoteToSelfChatEmpty                  True if the chat is empty. False, otherwise.
 * @property noteToSelfChatRoom                     Note to self chat room.
 */
data class NoteToSelfChatUIState(
    val noteToSelfChatRoom: ChatRoom? = null,
    val isNoteToSelfChatEmpty: Boolean = true,
) {
    /**
     * Get note to self chat id
     */
    val noteToSelfChatId
        get() = noteToSelfChatRoom?.chatId

}