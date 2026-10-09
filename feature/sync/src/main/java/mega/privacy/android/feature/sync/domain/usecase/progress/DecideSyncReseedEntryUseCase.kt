package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow

/**
 * Decides what one reseed entry does to the sync transfer store, from the tag's tracked row or
 * overflow entry as they are just before it, so a tag listed twice is merged the second time.
 * A completed tag is never admitted again.
 */
internal class DecideSyncReseedEntryUseCase @Inject constructor(
    private val mergeSyncTransferRowUseCase: MergeSyncTransferRowUseCase,
) {

    operator fun invoke(
        entry: SyncTransferEntry,
        isCompletedTag: Boolean,
        stored: SyncTransferRow?,
        parked: SyncTransferOverflowEntry?,
    ): ReseedStep = when {
        entry.state.isTerminal -> ReseedStep.Terminal
        isCompletedTag -> ReseedStep.Unchanged
        stored != null -> if (entry.notificationNumber > stored.entry.notificationNumber) {
            ReseedStep.Merge(mergeSyncTransferRowUseCase(stored, entry))
        } else {
            ReseedStep.Unchanged
        }

        parked != null -> ReseedStep.Place
        else -> ReseedStep.Admit
    }

    /** What the store does with one reseed entry. */
    sealed interface ReseedStep {
        data object Terminal : ReseedStep
        data object Unchanged : ReseedStep
        data class Merge(val row: SyncTransferRow) : ReseedStep
        data object Place : ReseedStep
        data object Admit : ReseedStep
    }
}
