package mega.privacy.android.domain.entity.contacts

import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Local contact domain entity
 *
 * @property id Contact's ID
 * @property name Contact's name
 * @property phoneNumbers List of contacts' phone numbers
 * @property normalizedPhoneNumbers List of contacts' normalized phone numbers
 * @property emails List of contacts' emails
 * @property photoUri The contact's photo Uri.
 */
data class LocalContact(
    val id: Long,
    val name: String = "",
    val phoneNumbers: List<String> = emptyList(),
    val normalizedPhoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val photoUri: UriPath? = null,
)
