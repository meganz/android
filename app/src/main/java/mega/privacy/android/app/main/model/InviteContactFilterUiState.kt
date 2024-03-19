package mega.privacy.android.app.main.model

import mega.privacy.android.app.main.InvitationContactInfo

/**
 * Ui state for filter functionality in InviteContactActivity
 *
 * @property filteredContacts List of filtered contacts based on user's query
 */
data class InviteContactFilterUiState(
    val filteredContacts: List<InvitationContactInfo> = emptyList(),
)
