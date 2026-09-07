package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares.model

import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesUiState

/**
 * UI state for the per-contact incoming shared folders screen.
 *
 * @property contactName Display name of the contact, or null while it is being resolved.
 * @property shares State of the contact's shared folders list, reused from the incoming shares tab.
 */
data class ContactSharedFoldersUiState(
    val contactName: String?,
    val shares: IncomingSharesUiState,
)
