package mega.privacy.android.data.mapper

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials.ContactCredentials

/**
 * Mapper to convert String credentials and other contact's data into [ContactCredentials]
 */
typealias ContactCredentialsMapper = (
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards ContactCredentials?

internal fun toContactCredentials(credentials: String?, email: String, name: String) =
    credentials?.getCredentials()?.let {
        ContactCredentials(
            credentials = it,
            email = email,
            name = name
        )
    }