package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry

/**
 * Orders a seed's entries for admission: terminal entries are dropped, slot holders come first,
 * then ascending priority and tag, so the slot holders are tracked before the tracked limit fills.
 */
internal class OrderSyncTransferSeedUseCase @Inject constructor() {

    operator fun invoke(entries: List<SyncTransferEntry>): List<SyncTransferEntry> =
        entries.filterNot { it.state.isTerminal }
            .sortedWith(
                compareBy<SyncTransferEntry>(
                    { !it.state.isSlotHolding },
                    { it.priority },
                    { it.tag },
                ),
            )
}
