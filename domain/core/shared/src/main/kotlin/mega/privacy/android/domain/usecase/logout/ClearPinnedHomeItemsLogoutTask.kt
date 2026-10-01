package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.home.ClearPinnedHomeItemsUseCase
import javax.inject.Inject

/**
 * Clears the user's pinned Home items on logout so the next session starts clean.
 */
class ClearPinnedHomeItemsLogoutTask @Inject constructor(
    private val clearPinnedHomeItemsUseCase: ClearPinnedHomeItemsUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearPinnedHomeItemsUseCase()
    }
}
