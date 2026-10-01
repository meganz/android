package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.viewedlinks.ClearViewedLinksUseCase
import javax.inject.Inject

/**
 * Clear recently viewed links logout task
 */
class ClearViewedLinksLogoutTask @Inject constructor(
    private val clearViewedLinksUseCase: ClearViewedLinksUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearViewedLinksUseCase()
    }
}
