package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.domain.entity.transfer.TransferState

/**
 * Decides what a reseed does to a tracked row from before it. A row the reseed lists, or one
 * newer than its watermark, is left alone; any other row ended while the snapshot was read.
 */
internal class DecideSyncStaleTrackedRowUseCase @Inject constructor() {

    operator fun invoke(
        state: TransferState,
        notificationNumber: Long,
        watermark: Long,
        isPresent: Boolean,
        isPreviouslyKept: Boolean,
    ): StaleStep = when {
        isPresent || notificationNumber > watermark -> StaleStep.Untouched
        state == TransferState.STATE_COMPLETING && !isPreviouslyKept -> StaleStep.KeepCompleting
        else -> StaleStep.Remove
    }

    /** What the reseed does with a tracked row from before it. */
    enum class StaleStep { Untouched, KeepCompleting, Remove }

    companion object {
        /**
         * The notification number a reseed's roll-backs carry. Stale rows ended before the
         * snapshot's new rows began, so their roll-backs reach the counters before the reseed's
         * admissions, and a batch that empties resets before the new rows join.
         */
        const val RESEED_ROLL_BACK_ORDER = Long.MIN_VALUE
    }
}
