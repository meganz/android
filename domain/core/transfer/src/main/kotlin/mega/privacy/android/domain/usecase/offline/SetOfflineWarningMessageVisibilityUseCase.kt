package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * SetOfflineWarningMessageVisibility
 *
 */
class SetOfflineWarningMessageVisibilityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     * @param isVisible boolean of the visibility of the offline warning
     */
    suspend operator fun invoke(isVisible: Boolean) =
        settingsRepository.setOfflineWarningMessageVisibility(isVisible)
}