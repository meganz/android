package mega.privacy.android.shared.contact.model

import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Contact permission ui state
 *
 * @property contactItemUiState
 * @property permission
 */
data class ContactPermissionUiState(
    val contactItemUiState: ContactItemUiState,
    val email: String,
    val permission: AccessPermission,
)
