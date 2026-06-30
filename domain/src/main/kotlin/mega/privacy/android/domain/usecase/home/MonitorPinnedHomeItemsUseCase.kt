package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor the items pinned to the Home screen, ordered oldest-pinned first.
 */
class MonitorPinnedHomeItemsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke() = settingsRepository.monitorPinnedHomeItems()
}
