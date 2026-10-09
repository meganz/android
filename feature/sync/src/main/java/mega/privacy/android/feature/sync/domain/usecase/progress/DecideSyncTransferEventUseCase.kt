package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow

/**
 * Decides what one feed event does to the sync transfer store, from the tag's tracked row or
 * overflow entry as they are just before it. Only a tracked row has a number to be stale against;
 * an overflow entry keeps none.
 */
internal class DecideSyncTransferEventUseCase @Inject constructor(
    private val mergeSyncTransferRowUseCase: MergeSyncTransferRowUseCase,
) {

    operator fun invoke(
        event: SyncTransferEvent,
        stored: SyncTransferRow?,
        parked: SyncTransferOverflowEntry?,
        isRemovedByReseed: Boolean,
    ): EventStep {
        val entry = event.entry
        val n = entry.notificationNumber
        val isTerminal = event.type == SyncTransferEventType.Finish || entry.state.isTerminal
        val isCompletion = isTerminal && entry.state == TransferState.STATE_COMPLETED
        return when {
            stored != null -> when {
                n <= stored.entry.notificationNumber -> EventStep.Stale
                isCompletion -> EventStep.Complete(stored.mediaType)
                isTerminal -> EventStep.RollBack(stored.entry.direction, stored.mediaType)
                else -> EventStep.Merge(mergeSyncTransferRowUseCase(stored, entry))
            }

            parked != null -> when {
                isCompletion -> EventStep.Complete(parked.mediaType)
                isTerminal -> EventStep.RollBack(parked.direction, parked.mediaType)
                else -> EventStep.Place
            }

            isCompletion && (event.startNotificationNumber != null || isRemovedByReseed) ->
                EventStep.AdmitAndComplete(event.startNotificationNumber ?: n)

            isTerminal -> EventStep.Ignore
            else -> EventStep.Admit(event.startNotificationNumber ?: n)
        }
    }

    /** What the store does with one event. */
    sealed interface EventStep {
        data object Stale : EventStep
        data class Complete(val mediaType: SyncMediaType) : EventStep
        data class RollBack(val direction: SyncTransferDirection, val mediaType: SyncMediaType) : EventStep

        data class Merge(val row: SyncTransferRow) : EventStep
        data object Place : EventStep
        data class AdmitAndComplete(val startedAt: Long) : EventStep
        data object Ignore : EventStep
        data class Admit(val at: Long) : EventStep
    }
}
