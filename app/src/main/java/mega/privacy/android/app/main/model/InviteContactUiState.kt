package mega.privacy.android.app.main.model

import mega.privacy.android.domain.entity.Feature

/**
 * State for invite Contact
 *
 * @property onContactsInitialized True if successfully initialized contacts, false otherwise
 */
data class InviteContactUiState(
    val onContactsInitialized: Boolean = false,
)
