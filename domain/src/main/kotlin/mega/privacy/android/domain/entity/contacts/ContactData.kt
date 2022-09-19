package mega.privacy.android.domain.entity.contacts

/**
 * Data class containing the main data of a contact.
 *
 * @property fullName             Contact full name.
 * @property alias                Contact alias.
 * @property avatarUri            Contact avatar uri.
 * @property defaultAvatarContent User default avatar letter.
 */
data class ContactData(
    val fullName: String?,
    val alias: String?,
    val avatarUri: String?,
    val defaultAvatarContent: String,
)
