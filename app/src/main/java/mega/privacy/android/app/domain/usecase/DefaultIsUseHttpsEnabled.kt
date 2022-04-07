package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default is use https enabled implementation
 *
 * @property settingsRepository
 */
class DefaultIsUseHttpsEnabled @Inject constructor(private val settingsRepository: SettingsRepository) : IsUseHttpsEnabled {
    override suspend fun invoke(): Boolean = settingsRepository.isUseHttpsPreferenceEnabled()
}