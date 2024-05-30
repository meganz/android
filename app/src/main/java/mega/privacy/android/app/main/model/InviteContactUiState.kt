package mega.privacy.android.app.main.model

/**
 * State for invite Contact
 *
 * @property onContactsInitialized True if successfully initialized contacts, false otherwise
 * @property contactLink The generated contact link for the invitation
 * @property invitationStatus The ui state for the invitation status.
 */
data class InviteContactUiState(
    val onContactsInitialized: Boolean = false,
    val contactLink: String = "",
    val invitationStatus: InvitationStatusUiState = InvitationStatusUiState(),
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
