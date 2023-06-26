package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Set hide recent activity preference
 */
class SetHideRecentActivityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @param hide
     */
    suspend operator fun invoke(hide: Boolean) =
        settingsRepository.setHideRecentActivity(hide)
}
