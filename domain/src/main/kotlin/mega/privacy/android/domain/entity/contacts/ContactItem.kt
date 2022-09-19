package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.user.UserVisibility
import java.io.Serializable

/**
 * Data class of a MEGA user.
 *
 * @property handle                 User identifier.
 * @property email                  User email.
 * @property fullName               User full name.
 * @property alias                  User alias.
 * @property defaultAvatarContent   User default avatar letter.
 * @property defaultAvatarColor     User default avatar color.
 * @property visibility             [UserVisibility].
 * @property timestamp              Time when the user was included in the contact list.
 * @property areCredentialsVerified True if user credentials are verified, false otherwise.
 * @property status                 [UserStatus].
 * @property avatarUri              User avatar uri.
 * @property lastSeen               User last seen.
 */
data class ContactItem(
    val handle: Long,
    val email: String,
    val fullName: String? = null,
    val alias: String? = null,
    val defaultAvatarContent: String,
    val defaultAvatarColor: String,
    val visibility: UserVisibility,
    val timestamp: Long,
    val areCredentialsVerified: Boolean,
    val status: UserStatus,
    val avatarUri: String? = null,
    val lastSeen: Int? = null,
) : Serializable
