package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials.ContactCredentials
import javax.inject.Inject

/**
 * Mapper to convert data to [ContactCredentials]
 */
internal class ContactCredentialsMapper @Inject constructor() {
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
    ) = credentials?.getCredentials()?.let {
        ContactCredentials(
            credentials = it,
            email = email,
            name = name
        )
    }
}