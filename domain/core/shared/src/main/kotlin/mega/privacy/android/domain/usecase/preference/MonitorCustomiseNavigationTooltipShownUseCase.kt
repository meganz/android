package mega.privacy.android.domain.usecase.preference

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to monitor whether the customise navigation tooltip has been shown
 */
class MonitorCustomiseNavigationTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return a flow emitting true if the tooltip has been shown, false otherwise
     */
    operator fun invoke(): Flow<Boolean> =
        settingsRepository.monitorCustomiseNavigationTooltipShown()
}
