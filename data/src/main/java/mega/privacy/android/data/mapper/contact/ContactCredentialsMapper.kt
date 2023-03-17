package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.AccountCredentials.ContactCredentials

/**
 * Mapper to convert data to [ContactCredentials]
 */
internal fun interface ContactCredentialsMapper {
    /**
     * Invoke
     *
     * @param credentials String containing contact credentials.
     * @param email Contact email.
     * @param name Contact name.
     * @return [ContactCredentials]
     */
    operator fun invoke(
        credentials: String?,
        email: String,
        name: String,
    ): ContactCredentials?
}