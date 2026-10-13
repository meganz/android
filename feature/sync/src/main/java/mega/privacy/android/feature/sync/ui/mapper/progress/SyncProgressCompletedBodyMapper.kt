package mega.privacy.android.feature.sync.ui.mapper.progress

import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import javax.inject.Inject

/**
 * Maps the Completed projection to the body of the Completed tab. The tab does not wait for the
 * episode status because its rows come from the store alone.
 */
internal class SyncProgressCompletedBodyMapper @Inject constructor(
    private val completedMapper: SyncCompletedTransferUiModelMapper,
) {

    operator fun invoke(list: SyncTransferList?, isRootNodeReady: Boolean): SyncProgressCompletedBody {
        if (list == null || !list.isSeeded || !isRootNodeReady) return SyncProgressCompletedBody.Loading
        val finished = list.items.filterIsInstance<SyncTransferListItem.Finished>()
        if (finished.isEmpty()) return SyncProgressCompletedBody.Empty
        return SyncProgressCompletedBody.Transfers(
            items = finished.map { completedMapper(it.transfer) }.toImmutableList(),
        )
    }
}
