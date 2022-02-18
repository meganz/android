package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.UserAccount

interface CanDeleteAccount {
    operator fun invoke(account: UserAccount): Boolean
}