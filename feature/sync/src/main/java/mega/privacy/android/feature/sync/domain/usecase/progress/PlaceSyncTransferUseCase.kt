package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore

/**
 * Decides whether a transfer is tracked as a row or kept in the compact overflow. Below the
 * tracked limit every state is tracked; at or above it only a slot holder, within the headroom.
 * Tracked rows are never demoted, so this bound holds only if every put into them asks here.
 */
internal class PlaceSyncTransferUseCase @Inject constructor() {

    operator fun invoke(
        entry: SyncTransferEntry,
        mediaType: SyncMediaType,
        parked: SyncTransferOverflowEntry?,
        activeCount: Int,
        trackedLimit: Int,
    ): Placement = when {
        hasRoom(entry.state, activeCount, trackedLimit) -> Placement.Track
        parked == null -> Placement.Park(SyncTransferOverflowEntry(entry.direction, entry.state, mediaType))
        parked.state != entry.state -> Placement.Park(parked.copy(state = entry.state))
        // A copy of an equal entry would only churn the map.
        else -> Placement.Keep
    }

    private fun hasRoom(state: TransferState, activeCount: Int, trackedLimit: Int): Boolean =
        activeCount < trackedLimit ||
                (state.isSlotHolding &&
                        activeCount < trackedLimit + SyncTransferStore.SLOT_HOLDER_HEADROOM)

    /** Where a transfer goes: a tracked row, a new or updated overflow entry, or nowhere new. */
    sealed interface Placement {
        data object Track : Placement
        data class Park(val entry: SyncTransferOverflowEntry) : Placement
        data object Keep : Placement
    }
}
