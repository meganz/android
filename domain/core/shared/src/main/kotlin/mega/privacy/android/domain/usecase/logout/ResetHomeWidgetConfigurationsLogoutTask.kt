package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.usecase.home.ResetHomeWidgetConfigurationsUseCase
import javax.inject.Inject

/**
 * Resets the user's home widget configurations on logout so the next session starts from defaults.
 */
class ResetHomeWidgetConfigurationsLogoutTask @Inject constructor(
    private val resetHomeWidgetConfigurationsUseCase: ResetHomeWidgetConfigurationsUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        resetHomeWidgetConfigurationsUseCase()
    }
}
