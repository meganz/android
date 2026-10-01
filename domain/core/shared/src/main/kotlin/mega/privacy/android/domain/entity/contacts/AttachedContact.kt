package mega.privacy.android.domain.entity.contacts

/**
 * A single contact attached to a chat message.
 *
 * @property handle Contact user handle.
 * @property email Contact email.
 * @property name Contact name as stored in the message.
 * @property isMe True when the attached contact is the current user.
 * @property isContact True when the attached user is a visible contact of the current user.
 * @property isVerified True when the contact's credentials are verified.
 * @property avatarColor Color used as the background of the contact's default avatar.
 */
data class AttachedContact(
    val handle: Long,
    val email: String,
    val name: String,
    val isMe: Boolean,
    val isContact: Boolean,
    val isVerified: Boolean,
    val avatarColor: Int,
)
