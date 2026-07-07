package mega.privacy.android.domain.usecase.home

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitors the persisted sort preference for the pinned home items View-all list.
 */
class MonitorPinnedItemsSortPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<Pair<PinnedHomeItemsSortField, SortDirection>> =
        settingsRepository.monitorPinnedItemsSortPreference()
}
