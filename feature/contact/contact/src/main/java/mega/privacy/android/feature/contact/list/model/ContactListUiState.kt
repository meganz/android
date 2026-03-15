package mega.privacy.android.feature.contact.list.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * UI state for the contact list screen.
 */
@Stable
sealed interface ContactListUiState {

    /**
     * Initial loading state.
     */
    data object Loading : ContactListUiState

    /**
     * Data state containing contacts and events.
     *
     * @property contacts Contacts grouped by first character of display name.
     * @property recentlyAddedContacts Contacts added within the last 3 days with no chatroom.
     * @property incomingRequestCount Number of incoming contact requests.
     * @property openChatEvent One-shot event to open a chat by chatId.
     * @property startCallEvent One-shot event to start or join a call.
     */
    data class Data(
        val contacts: Map<String, List<ContactUiModel>>,
        val recentlyAddedContacts: List<ContactUiModel>,
        val incomingRequestCount: Int,
        val openChatEvent: StateEventWithContent<Long>,
        val startCallEvent: StateEventWithContent<CallEventData>,
    ) : ContactListUiState
}

/**
 * Data for a call event emission.
 *
 * @property chatId Chat id for the call.
 * @property hasLocalAudio Whether local audio is enabled.
 * @property hasLocalVideo Whether local video is enabled.
 * @property isExistingCall Whether there is already an active call in the chat.
 */
data class CallEventData(
    val chatId: Long,
    val hasLocalAudio: Boolean,
    val hasLocalVideo: Boolean,
    val isExistingCall: Boolean,
)

/**
 * Pure UI model for a contact. No Android framework types.
 *
 * @property handle User handle.
 * @property email User email.
 * @property displayName Resolved display name (alias > fullName > email).
 * @property fullName Full name if available.
 * @property alias Alias if available.
 * @property status User chat status.
 * @property avatarUri Avatar URI string if available.
 * @property avatarColor Default avatar color as hex string.
 * @property lastSeenSeconds Raw last seen value in seconds.
 * @property isNew Whether the contact was recently added (within 3 days, no chatroom).
 * @property isVerified Whether the contact's credentials are verified.
 */
data class ContactUiModel(
    val handle: Long,
    val email: String,
    val displayName: String,
    val fullName: String?,
    val alias: String?,
    val status: UserChatStatus,
    val avatarUri: String?,
    val avatarColor: String?,
    val lastSeenSeconds: Int?,
    val isNew: Boolean,
    val isVerified: Boolean,
)
