package mega.privacy.android.app.presentation.achievements.invites.model

/**
 * InviteFriendsUIState
 * @property grantStorageInBytes the value of storage granted from inviting friends in bytes
 * @property isNewInviteContactActivityEnabled Whether the new invite contact activity flag is enabled.
 */
data class InviteFriendsUIState(
    val grantStorageInBytes: Long = 0,
    val isNewInviteContactActivityEnabled: Boolean = false,
)