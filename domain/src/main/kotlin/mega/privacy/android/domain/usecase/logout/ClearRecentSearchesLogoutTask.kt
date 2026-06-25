package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.search.ClearRecentSearchesUseCase
import javax.inject.Inject

/**
 * Clear recent searches logout task
 */
class ClearRecentSearchesLogoutTask @Inject constructor(
    private val clearRecentSearchesUseCase: ClearRecentSearchesUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearRecentSearchesUseCase()
    }
}
