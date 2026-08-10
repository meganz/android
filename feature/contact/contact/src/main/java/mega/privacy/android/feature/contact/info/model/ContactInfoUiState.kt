package mega.privacy.android.feature.contact.info.model

import de.palm.composestateevents.StateEvent
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.shared.contact.model.AvatarData

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
     * @property displayName Name to display for the contact (full name or email).
     * @property nickname Nickname (alias) set for the contact, or null when none is set.
     * @property email Email of the contact, or null for a non-contact chat peer.
     * @property userHandle Handle of the contact.
     * @property chatRoomId Id of the 1:1 chat room with the contact, or null if none exists.
     * @property isFromContacts True when the screen was opened from the contact list (by email),
     * false when opened from a chat (by chat id).
     * @property avatar Avatar of the contact (image file or coloured initials).
     * @property userChatStatus Chat presence status of the contact.
     * @property lastSeenMinutes Minutes elapsed since the contact was last seen, or null if unknown.
     * @property areCredentialsVerified True when the contact's credentials are verified.
     * @property isNotificationEnabled True when chat notifications are enabled, false when muted,
     * null when unknown (no chat or not yet loaded).
     * @property retentionTimeSeconds Chat history retention time in seconds, or null when unknown.
     * @property inSharesCount Number of folders the contact shares with the user.
     * @property enableCallButtons True when the audio/video call buttons can be used.
     * @property isOnline True when the device is connected to the internet.
     * @property closeEvent
     */
    data class Data(
        val displayName: String,
        val nickname: String?,
        val email: String?,
        val userHandle: Long,
        val chatRoomId: Long?,
        val isFromContacts: Boolean,
        val avatar: AvatarData,
        val userChatStatus: UserChatStatus,
        val lastSeenMinutes: Int?,
        val areCredentialsVerified: Boolean,
        val isNotificationEnabled: Boolean?,
        val retentionTimeSeconds: Long?,
        val inSharesCount: Int,
        val enableCallButtons: Boolean,
        val isOnline: Boolean,
        override val closeEvent: StateEvent,
    ) : ContactInfoUiState {

        /**
         * Whether the message/call action buttons are shown. Offline the actions are kept visible
         * (calls are disabled separately); online they require a known email.
         */
        val showChatOptions: Boolean get() = !isOnline || email != null

        /**
         * Whether the share contact row is shown.
         */
        val showShareContact: Boolean get() = isOnline && email != null

        /**
         * Whether the shared folders row is shown.
         */
        val showSharedFolders: Boolean get() = isOnline && email != null

        /**
         * Whether the verify credentials row is shown.
         */
        val showVerifyCredentials: Boolean get() = !email.isNullOrEmpty()

        /**
         * Whether the chat shared files row is shown.
         */
        val showSharedFiles: Boolean get() = chatRoomId != null

        /**
         * Whether the manage chat history row is shown.
         */
        val showManageChatHistory: Boolean get() = chatRoomId != null && isOnline
    }
}
