package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.setting.GetMiscFlagsUseCase
import javax.inject.Inject

/**
 * Load misc flags logout task
 */
class LoadMiscFlagsTask @Inject constructor(
    private val getMiscFlagsUseCase: GetMiscFlagsUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        getMiscFlagsUseCase()
    }
}