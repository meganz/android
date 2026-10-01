package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to check if the video editor tooltip has been shown
 */
class MonitorVideoEditorTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return true if the tooltip has been shown, false otherwise
     */
    operator fun invoke() = settingsRepository.monitorVideoEditorTooltipShown()
}
