package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.Contact

/**
 * The domain entity represents the user's contact
 *
 * @property contact The [Contact] of the user.
 * @property user The [User] corresponds to the [Contact].
 */
data class UserContact(
    val contact: Contact?,
    val user: User?,
)
