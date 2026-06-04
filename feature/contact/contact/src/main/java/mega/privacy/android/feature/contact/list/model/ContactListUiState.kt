package mega.privacy.android.feature.contact.list.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent
import mega.privacy.android.shared.contact.model.ContactItemUiState

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
        val contacts: Map<String, List<ContactItemUiState>>,
        val recentlyAddedContacts: List<ContactItemUiState>,
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
