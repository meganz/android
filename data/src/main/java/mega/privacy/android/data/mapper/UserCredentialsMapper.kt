package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.UserCredentials


internal typealias UserCredentialsMapper = (
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
) -> @JvmSuppressWildcards UserCredentials

internal fun toUserCredentialsMapper(
    email: String?,
    session: String?,
    firstName: String?,
    lastName: String?,
    myHandle: String?,
) = UserCredentials(
    email = email,
    session = session,
    firstName = firstName,
    lastName = lastName,
    myHandle = myHandle
)