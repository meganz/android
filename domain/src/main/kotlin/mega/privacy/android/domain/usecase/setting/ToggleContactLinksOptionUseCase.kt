package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Toggle auto accept setting for incoming contact requests using contact links setting use case.
 *
 */
class ToggleContactLinksOptionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return the new value of the setting
     */
    suspend operator fun invoke() =
        settingsRepository.setContactLinksOption(!settingsRepository.getContactLinksOption())
}
