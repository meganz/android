package mega.privacy.android.app.main.model

/**
 * State for invite Contact
 *
 * @property onContactsInitialized True if successfully initialized contacts, false otherwise
 * @property contactLink The generated contact link for the invitation
 */
data class InviteContactUiState(
    val onContactsInitialized: Boolean = false,
    val contactLink: String = "",
)
