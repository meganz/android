package mega.privacy.android.app.presentation.meeting.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Presentational UI state for a single participant row in the Chat Info screen.
 *
 * @property contactItem Pre-resolved row data (display name, status, avatar, last seen, verified).
 * @property isMe True when the row represents the local user.
 * @property privilege Chat-room privilege for permission iconography and the change-permission action.
 * @property email Used by the invite-contact action when triggered from the participant menu.
 * @property avatarUpdateTimestamp Cache-busting key for the underlying avatar image.
 */
@Stable
data class ChatParticipantUiState(
    val contactItem: ContactItemUiState,
    val isMe: Boolean,
    val privilege: ChatRoomPermission,
    val email: String?,
    val avatarUpdateTimestamp: Long?,
) {
    /** Stable participant identifier; delegates to [contactItem]. */
    val handle: Long get() = contactItem.handle
}
