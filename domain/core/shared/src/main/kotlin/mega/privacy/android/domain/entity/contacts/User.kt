package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Basic data for a MegaUser.
 *
 * @property handle
 * @property email
 * @property visibility
 * @property timestamp
 * @property userChanges
 */
data class User(
    val handle: Long,
    val email: String,
    val visibility: UserVisibility,
    val timestamp: Long,
    val userChanges: List<UserChanges>,
)