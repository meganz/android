package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import javax.inject.Inject

/**
 * Load misc flags logout task
 */
class LoadMiscFlagsTask @Inject constructor(
    private val getMiscFlagsUseCase: GetMiscFlagsUseCase,
    private val accountRepository: AccountRepository,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        accountRepository.broadcastMiscUnLoaded()
        getMiscFlagsUseCase()
    }
}