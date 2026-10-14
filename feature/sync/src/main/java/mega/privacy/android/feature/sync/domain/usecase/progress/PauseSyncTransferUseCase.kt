package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Pauses or resumes one sync transfer row by tag, with an optimistic state that shows at once. */
internal class PauseSyncTransferUseCase @Inject constructor(
    private val syncTransferStoreRepository: SyncTransferStoreRepository,
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase,
    private val getSyncTransferRowStateUseCase: GetSyncTransferRowStateUseCase,
) {

    /**
     * Returns false when the row's state does not allow the request, or when the request fails.
     * A failed request reverts the optimistic state. The request and the revert run under
     * [NonCancellable]; a [CancellationException] from the request is rethrown after the revert.
     */
    suspend operator fun invoke(tag: Int, isPause: Boolean): Boolean {
        val row = syncTransferStoreRepository.monitorSyncTransferStore().first().active[tag]
            ?: return false
        val isOwnPause = getSyncTransferRowStateUseCase(
            row = row,
            context = neutralRowContext,
            directionSpeed = null,
        ).isOwnPause
        val readsPausable = row.entry.state in pausableStates || row.optimisticPause == false
        val isAllowed = if (isPause) readsPausable && !isOwnPause else isOwnPause
        if (!isAllowed) return false
        // The user can leave the screen during the request, and an optimistic state without its
        // request or its revert would stay on the row until the SDK state changes.
        return withContext(NonCancellable) {
            syncTransferStoreRepository.setOptimisticPause(setOf(tag), isPause)
            try {
                // The result is the echoed pause flag, not success: a failed request throws.
                pauseTransferByTagUseCase(tag, isPause)
                true
            } catch (e: Throwable) {
                // Catch `Throwable` because a failed SDK request throws `MegaException`, which
                // does not extend `Exception`.
                syncTransferStoreRepository.revertOptimisticPause(tag)
                if (e is CancellationException) throw e
                Timber.w(e, "Failed to pause or resume a sync transfer")
                false
            }
        }
    }

    private companion object {
        val pausableStates = setOf(
            TransferState.STATE_QUEUED,
            TransferState.STATE_ACTIVE,
            TransferState.STATE_RETRYING,
        )

        // Own pause does not read the context, so any context gives the same value.
        val neutralRowContext = SyncTransferRowContext(
            areTransfersPaused = false,
            isStorageOverQuota = false,
            isTransferOverQuota = false,
        )
    }
}
