package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NetworkRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default set use https implementation
 *
 * @property settingsRepository
 * @property networkRepository
 */
class DefaultSetUseHttps @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val networkRepository: NetworkRepository,
) : SetUseHttps {
    override suspend fun invoke(enabled: Boolean): Boolean {
        settingsRepository.setUseHttpsPreference(enabled)
        networkRepository.setUseHttps(enabled)

        return enabled
    }
}