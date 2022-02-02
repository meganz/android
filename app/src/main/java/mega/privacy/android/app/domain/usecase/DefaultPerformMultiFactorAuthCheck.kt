package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.AccountRepository
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultPerformMultiFactorAuthCheck @Inject constructor(private val accountRepository: AccountRepository) : PerformMultiFactorAuthCheck {
    override fun invoke(request: MegaRequestListenerInterface) {
        accountRepository.fetchMultiFactorAuthConfiguration(request)
    }
}