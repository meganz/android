package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Clears all recently used items.
 */
class ClearRecentlyUsedItemsUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke() =
        repository.clearAllRecentlyUsedItems()
}
