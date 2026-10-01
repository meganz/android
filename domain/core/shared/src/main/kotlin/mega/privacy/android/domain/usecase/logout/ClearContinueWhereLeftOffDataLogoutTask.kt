package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.continuewhereleftoff.ClearRecentlyUsedItemsUseCase
import javax.inject.Inject

/**
 * Clear continue where left off data logout task
 */
class ClearContinueWhereLeftOffDataLogoutTask @Inject constructor(
    private val clearRecentlyUsedItemsUseCase: ClearRecentlyUsedItemsUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearRecentlyUsedItemsUseCase()
    }
}
