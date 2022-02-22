package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount

interface GetAccountDetails {
    operator fun invoke(forceRefresh: Boolean): UserAccount
}