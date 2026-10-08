package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.first
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/** Clears every completed sync transfer a [SyncTransferFilter] shows, beyond the window too. */
internal class ClearVisibleCompletedSyncTransfersUseCase @Inject constructor(
    private val monitorSyncTransferListUseCase: MonitorSyncTransferListUseCase,
    private val syncTransferStoreRepository: SyncTransferStoreRepository,
) {
    suspend operator fun invoke(filter: SyncTransferFilter) {
        // A media or search filter lists its active rows first, so a capped window would
        // leave completed rows out.
        val tags = monitorSyncTransferListUseCase(filter, limit = Int.MAX_VALUE).first()
            .items
            .filterIsInstance<SyncTransferListItem.Finished>()
            .mapTo(mutableSetOf()) { it.tag }
        if (tags.isNotEmpty()) syncTransferStoreRepository.clearCompleted(tags)
    }
}
