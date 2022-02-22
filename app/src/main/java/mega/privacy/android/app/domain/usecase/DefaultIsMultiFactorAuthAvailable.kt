package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import javax.inject.Inject

class DefaultIsMultiFactorAuthAvailable @Inject constructor(private val accountRepository: AccountRepository) : IsMultiFactorAuthAvailable {
    override fun invoke(): Boolean {
        return accountRepository.isMultiFactorAuthAvailable()
    }
}