package mega.privacy.android.app.main.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult

/**
 * State for invite Contact
 *
 * @property isLoading Whether the contact list is initializing.
 * @property areContactsInitialized True if successfully initialized contacts, false otherwise
 * @property contactLink The generated contact link for the invitation
 * @property selectedContactInformation List of the selected contact information.
 * @property showOpenCameraConfirmation Whether we need to show the open camera confirmation.
 * @property shouldInitializeQR Whether we need to initialize the QR scanner.
 * @property filteredContacts List of filtered contacts based on user's query
 * @property pendingPhoneNumberInvitations The list of pending phone number invitations that should be invited via SMS.
 * @property query The input query.
 * @property invitationContactInfoWithMultipleContacts A contact info with multiple contacts.
 * @property invitationStatusResult The result of the invitations.
 * @property emailValidationMessage The email validation result message.
 */
@Immutable
data class InviteContactUiState(
    val isLoading: Boolean = false,
    val areContactsInitialized: Boolean = false,
    val contactLink: String = "",
    val selectedContactInformation: List<InvitationContactInfo> = emptyList(),
    val showOpenCameraConfirmation: Boolean = false,
    val shouldInitializeQR: Boolean = false,
    val filteredContacts: List<InvitationContactInfo> = emptyList(),
    val pendingPhoneNumberInvitations: List<String> = emptyList(),
    val query: String = "",
    val invitationContactInfoWithMultipleContacts: InvitationContactInfo? = null,
    val invitationStatusResult: InvitationStatusMessageUiState? = null,
    val emailValidationMessage: MessageTypeUiState? = null,
) {
    /**
     * A UI state represents the invitation status message
     */
    sealed interface InvitationStatusMessageUiState {

        /**
         * The invitations sent.
         *
         * @property messages The messages.
         */
        data class InvitationsSent(
            val messages: List<MessageTypeUiState>,
        ) : InvitationStatusMessageUiState

        /**
         * Should navigate up with result.
         *
         * @property result The result.
         */
        data class NavigateUpWithResult(
            val result: InviteContactScreenResult,
        ) : InvitationStatusMessageUiState
    }

    /**
     * The message type used for the [InviteContactUiState].
     */
    sealed interface MessageTypeUiState {

        /**
         * Singular string.
         *
         * @property id The string resource ID.
         * @property argument The string's argument.
         */
        data class Singular(
            @StringRes val id: Int,
            val argument: String? = null,
        ) : MessageTypeUiState

        /**
         * Plural string.
         *
         * @property id The string resource ID.
         * @property quantity The string quantity.
         */
        data class Plural(
            @PluralsRes val id: Int,
            val quantity: Int,
        ) : MessageTypeUiState
    }
}
