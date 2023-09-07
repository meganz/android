package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Set SubFolder MediaDiscovery Enabled
 *
 */
class SetSubFolderMediaDiscoveryEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(enabled: Boolean) =
        settingsRepository.setSubfolderMediaDiscoveryEnabled(enabled)
}