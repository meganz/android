package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * MonitorSubFolderMediaDiscoverySettingsUseCase
 */
class MonitorSubFolderMediaDiscoverySettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke() =
        settingsRepository.monitorSubfolderMediaDiscoveryEnabled().map { it ?: false }
}