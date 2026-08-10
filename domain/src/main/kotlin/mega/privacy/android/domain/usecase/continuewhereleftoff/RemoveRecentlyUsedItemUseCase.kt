package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Removes a single item from the recently used index.
 */
class RemoveRecentlyUsedItemUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(nodeHandle: Long) =
        repository.removeRecentlyUsedItem(nodeHandle)
}
