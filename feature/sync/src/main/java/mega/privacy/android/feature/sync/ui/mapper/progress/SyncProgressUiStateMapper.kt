package mega.privacy.android.feature.sync.ui.mapper.progress

import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressUiState
import javax.inject.Inject

/**
 * Assembles [SyncProgressUiState] from the inputs of the sync progress screen.
 */
internal class SyncProgressUiStateMapper @Inject constructor(
    private val bodyMapper: SyncProgressBodyMapper,
) {

    /**
     * Maps the inputs to the screen state. [pendingList] is the selected pending tab's
     * projection; [rowStates] and [bands] are keyed by tag and go to [SyncProgressBodyMapper].
     * [completedBody] comes mapped, because the Completed list changes far less often than the rest.
     */
    operator fun invoke(
        status: SyncProgressStatus,
        pendingList: SyncTransferList?,
        completedList: SyncTransferList?,
        completedBody: SyncProgressCompletedBody,
        blockers: SyncProgressBlockers,
        isRootNodeReady: Boolean,
        rowStates: Map<Int, SyncTransferRowState>,
        bands: Map<Int, Int>,
        isRetryingTransferOverQuota: Boolean,
    ) = SyncProgressUiState(
        filesLeft = status.filesLeft,
        activeBody = bodyMapper(
            status = status,
            list = pendingList,
            blockers = blockers,
            isRootNodeReady = isRootNodeReady,
            rowStates = rowStates,
            bands = bands,
            isRetryingTransferOverQuota = isRetryingTransferOverQuota,
        ),
        completedBody = completedBody,
        areTransfersPaused = blockers.areTransfersPaused,
        // Every filter carries the same counters, so the Completed list fills them while the
        // pending projection is still on its way.
        filterCounts = (pendingList ?: completedList)?.counts ?: SyncTransferFilterCounts.Empty,
    )
}
