package mega.privacy.android.domain.entity.contacts

/**
 * Presence of a user in chat.
 *
 * @property status           Current chat online status of the user.
 * @property lastGreenMinutes Minutes since the user was last seen online, or null when it is
 *                            unknown or not applicable for the current status.
 */
data class UserPresence(
    val status: UserChatStatus,
    val lastGreenMinutes: Int?,
)
