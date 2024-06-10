package mega.privacy.android.app.main.model

import mega.privacy.android.app.main.InvitationContactInfo

/**
 * State for invite Contact
 *
 * @property onContactsInitialized True if successfully initialized contacts, false otherwise
 * @property contactLink The generated contact link for the invitation
 * @property invitationStatus The ui state for the invitation status.
 * @property selectedContactInformation List of the selected contact information.
 * @property showOpenCameraConfirmation Whether we need to show the open camera confirmation.
 * @property shouldInitializeQR Whether we need to initialize the QR scanner.
 */
data class InviteContactUiState(
    val onContactsInitialized: Boolean = false,
    val contactLink: String = "",
    val invitationStatus: InvitationStatusUiState = InvitationStatusUiState(),
    val selectedContactInformation: List<InvitationContactInfo> = emptyList(),
    val showOpenCameraConfirmation: Boolean = false,
    val shouldInitializeQR: Boolean = false,
)

/**
 * Ui state for the invitation status.
 *
 * @property emails The list of emails to be invited.
 * @property totalInvitationSent The total count of successfully sent invitations.
 */
data class InvitationStatusUiState(
    val emails: List<String> = emptyList(),
    val totalInvitationSent: Int = 0,
)
