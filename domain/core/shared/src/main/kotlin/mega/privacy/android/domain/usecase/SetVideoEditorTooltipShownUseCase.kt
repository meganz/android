package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to mark the video editor tooltip as shown
 */
class SetVideoEditorTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        settingsRepository.setVideoEditorTooltipShown(true)
    }
}
