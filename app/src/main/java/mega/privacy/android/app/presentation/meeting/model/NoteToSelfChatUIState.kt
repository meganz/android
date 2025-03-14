package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * UI state for note to self chat
 *
 * @property isNoteToYourselfFeatureFlagEnabled     True if the note to yourself feature flag is enabled, false if not.
 * @property isNoteToSelfChatEmpty                  True if the chat is empty. False, otherwise.
 * @property noteToSelfChatRoom                     Note to self chat room.
 * @property isNewFeature                           True if new label should displayed. False, if not.
 */
data class NoteToSelfChatUIState(
    val isNoteToYourselfFeatureFlagEnabled: Boolean = false,
    val noteToSelfChatRoom: ChatRoom? = null,
    val isNoteToSelfChatEmpty: Boolean = true,
    val isNewFeature: Boolean = false,
) {
    /**
     * Get note to self chat id
     */
    val noteToSelfChatId
        get() = noteToSelfChatRoom?.chatId

    /**
     * Check if it is archived
     */
    val isArchived
        get() = noteToSelfChatRoom?.isArchived
}