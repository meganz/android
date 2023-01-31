package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.account.AccountSession

internal typealias AccountSessionMapper = (
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards String?,
    @JvmSuppressWildcards Long?,
) -> @JvmSuppressWildcards AccountSession

internal fun toAccountSession(email: String?, session: String?, myHandle: Long?) =
    AccountSession(email = email, session = session, myHandle = myHandle ?: -1)