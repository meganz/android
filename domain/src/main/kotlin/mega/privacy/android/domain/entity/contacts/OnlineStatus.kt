package mega.privacy.android.domain.entity.contacts

/**
 * Chat online status.
 *
 * @property userHandle User handle of who the status is.
 * @property status     Status.
 * @property inProgress Whether the reported status is being set or it is definitive (only for your own changes).
 */
data class OnlineStatus(val userHandle: Long, val status: UserChatStatus, val inProgress: Boolean)