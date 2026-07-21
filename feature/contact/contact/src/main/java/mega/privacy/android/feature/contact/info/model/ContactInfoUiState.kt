package mega.privacy.android.feature.contact.info.model

import de.palm.composestateevents.StateEvent

/**
 * Contact info ui state
 */
sealed interface ContactInfoUiState {

    /**
     * One-shot event requesting the screen to close, fired when the contact cannot be resolved.
     * Lives on the sealed interface because resolution failure happens before [Data] exists.
     */
    val closeEvent: StateEvent

    /**
     * Loading
     *
     * @property closeEvent
     */
    data class Loading(
        override val closeEvent: StateEvent,
    ) : ContactInfoUiState

    /**
     * Data
     *
     * @property displayName Name to display for the contact (alias, full name or email).
     * @property email Email of the contact, or null for a non-contact chat peer.
     * @property userHandle Handle of the contact.
     * @property chatRoomId Id of the 1:1 chat room with the contact, or null if none exists.
     * @property isFromContacts True when the screen was opened from the contact list (by email),
     * false when opened from a chat (by chat id).
     * @property closeEvent
     */
    data class Data(
        val displayName: String,
        val email: String?,
        val userHandle: Long,
        val chatRoomId: Long?,
        val isFromContacts: Boolean,
        override val closeEvent: StateEvent,
    ) : ContactInfoUiState
}
