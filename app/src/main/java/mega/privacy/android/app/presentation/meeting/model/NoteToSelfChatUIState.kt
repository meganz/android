package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * UI state for note to self chat
 *
 * @property isNoteToYourselfFeatureFlagEnabled     True if the note to yourself feature flag is enabled, false if not.
 * @property isNoteToSelfChatEmpty                  True if the chat is empty. False, otherwise.
 * @property noteToSelfChatRoom                     Note to self chat room.
 * @property newFeatureLabelCounter                 Counter for new label.
 */
data class NoteToSelfChatUIState(
    val isNoteToYourselfFeatureFlagEnabled: Boolean = false,
    val noteToSelfChatRoom: ChatRoom? = null,
    val isNoteToSelfChatEmpty: Boolean = false,
    val newFeatureLabelCounter: Int = -1,
) {
    /**
     * Get note to self chat id
     */
    val noteToSelfChatId
        get() = noteToSelfChatRoom?.chatId

    /**
     * Check if new feature label should shown
     */
    val isNewFeature: Boolean
        get() = newFeatureLabelCounter > 0 && isNoteToSelfChatEmpty
}