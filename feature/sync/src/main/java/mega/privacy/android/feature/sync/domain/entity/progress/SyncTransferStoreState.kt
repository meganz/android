package mega.privacy.android.feature.sync.domain.entity.progress

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/** One immutable snapshot of every sync transfer seen in the session. */
internal data class SyncTransferStoreState(
    val isSeeded: Boolean,
    val active: ImmutableMap<Int, SyncTransferRow>,
    val overflow: ImmutableMap<Int, SyncTransferOverflowEntry>,
    val completed: ImmutableList<SyncCompletedTransfer>,
    val counts: SyncTransferCounts,
    val reseedRemoved: ImmutableSet<Int>,
    val reseedKeptCompleting: ImmutableSet<Int> = persistentSetOf(),
) {
    companion object {
        /** The state before the first seed: nothing tracked and every counter empty. */
        val NotSeeded: SyncTransferStoreState = SyncTransferStoreState(
            isSeeded = false,
            active = persistentHashMapOf(),
            overflow = persistentHashMapOf(),
            completed = persistentListOf(),
            counts = SyncTransferCounts.Empty,
            reseedRemoved = persistentHashSetOf(),
            reseedKeptCompleting = persistentHashSetOf(),
        )
    }
}
