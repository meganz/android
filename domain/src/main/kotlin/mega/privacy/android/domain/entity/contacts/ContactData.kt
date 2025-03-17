package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Data class containing the main data of a contact.
 *
 * @property fullName             Contact full name.
 * @property alias                Contact alias.
 * @property avatarUri            Contact avatar uri.
 * @property userVisibility     Visibility status of the contact
 */
data class ContactData(
    val fullName: String?,
    val alias: String?,
    val avatarUri: String?,
    val userVisibility: UserVisibility,
)
