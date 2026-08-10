package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Records that a file was opened or interacted with.
 */
class SaveRecentlyUsedItemUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(
        nodeHandle: Long,
        type: RecentlyUsedType,
        fileName: String,
    ) = repository.saveRecentlyUsedItem(nodeHandle, type, fileName)
}
