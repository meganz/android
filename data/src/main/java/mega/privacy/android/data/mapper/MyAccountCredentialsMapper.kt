package mega.privacy.android.data.mapper

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials.MyAccountCredentials

/**
 * Mapper to convert String credentials into [MyAccountCredentials]
 */
typealias MyAccountCredentialsMapper = (@JvmSuppressWildcards String?) -> @JvmSuppressWildcards MyAccountCredentials?

internal fun toMyAccountCredentials(credentials: String?) =
    (credentials?.getCredentials()?.let { MyAccountCredentials(it) })