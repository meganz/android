package mega.privacy.android.app.presentation.achievements.invites.model

/**
 * InviteFriendsUIState
 * @property grantStorageInBytes the value of storage granted from inviting friends in bytes
 * @property durationInDays the duration of the granted storage in days
 */
data class InviteFriendsUIState(
    val grantStorageInBytes: Long = 0,
    val durationInDays: Int = 365,
)