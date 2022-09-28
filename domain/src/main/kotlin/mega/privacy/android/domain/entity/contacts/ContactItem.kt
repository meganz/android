package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Data class of a MEGA user.
 *
 * @property handle                 User identifier.
 * @property email                  User email.
 * @property contactData            [ContactData].
 * @property defaultAvatarColor     User default avatar color.
 * @property visibility             [UserVisibility].
 * @property timestamp              Time when the user was included in the contact list.
 * @property areCredentialsVerified True if user credentials are verified, false otherwise.
 * @property status                 [UserStatus].
 * @property lastSeen               User last seen.
 */
data class ContactItem(
    val handle: Long,
    val email: String,
    val contactData: ContactData,
    val defaultAvatarColor: String,
    val visibility: UserVisibility,
    val timestamp: Long,
    val areCredentialsVerified: Boolean,
    val status: UserStatus,
    val lastSeen: Int? = null,
)
