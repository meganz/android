package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.contacts.ContactData

/**
 * Mapper to convert data to [ContactData]
 */
typealias ContactDataMapper = (
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
) -> ContactData

internal fun toContactData(
    fullName: String?,
    alias: String?,
    avatarUri: String?,
): ContactData = ContactData(
    fullName = fullName,
    alias = alias,
    avatarUri = avatarUri
)