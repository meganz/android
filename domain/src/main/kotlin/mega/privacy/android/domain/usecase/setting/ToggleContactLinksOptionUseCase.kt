package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.exception.SettingNotFoundException
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
     * @return
     */
    suspend operator fun invoke() = runCatching {
        settingsRepository.getContactLinksOption()
    }.onSuccess { current ->
        return settingsRepository.setContactLinksOption(!current)
    }.recover { error ->
        if (error is SettingNotFoundException) {
            return settingsRepository.setContactLinksOption(true)
        }
        throw error
    }.getOrThrow()
}