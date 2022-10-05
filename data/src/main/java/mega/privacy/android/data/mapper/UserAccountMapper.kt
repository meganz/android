package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.user.UserId

typealias UserAccountMapper = (
    @JvmSuppressWildcards UserId?,
    @JvmSuppressWildcards String,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards Boolean,
    @JvmSuppressWildcards AccountType?,
    @JvmSuppressWildcards String,
) -> @JvmSuppressWildcards UserAccount