package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.entity.home.PinnedHomeItemsSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persists the sort preference for the pinned home items View-all list.
 */
class SetPinnedItemsSortUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        sortField: PinnedHomeItemsSortField,
        sortDirection: SortDirection,
    ) = settingsRepository.setPinnedItemsSortPreference(sortField, sortDirection)
}
