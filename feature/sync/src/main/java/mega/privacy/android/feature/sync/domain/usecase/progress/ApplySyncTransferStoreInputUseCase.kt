package mega.privacy.android.feature.sync.domain.usecase.progress

import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTransition
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncReseedEntryUseCase.ReseedStep
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncStaleTrackedRowUseCase.StaleStep
import mega.privacy.android.feature.sync.domain.usecase.progress.DecideSyncTransferEventUseCase.EventStep
import mega.privacy.android.feature.sync.domain.usecase.progress.PlaceSyncTransferUseCase.Placement
import mega.privacy.android.feature.sync.domain.usecase.progress.SettleSyncTransferCompletionsUseCase.Completion
import mega.privacy.android.feature.sync.domain.usecase.progress.SettleSyncTransferCompletionsUseCase.Emission

/** Folds one store input into the next state, applying desktop's sync transfer rules. */
internal class ApplySyncTransferStoreInputUseCase @Inject constructor(
    private val getSyncMediaTypeUseCase: GetSyncMediaTypeUseCase,
    private val placeSyncTransferUseCase: PlaceSyncTransferUseCase,
    private val orderSyncTransferSeedUseCase: OrderSyncTransferSeedUseCase,
    private val decideSyncTransferEventUseCase: DecideSyncTransferEventUseCase,
    private val decideSyncReseedEntryUseCase: DecideSyncReseedEntryUseCase,
    private val decideSyncStaleTrackedRowUseCase: DecideSyncStaleTrackedRowUseCase,
    private val decideOptimisticPauseUseCase: DecideOptimisticPauseUseCase,
    private val planSyncTransferClearUseCase: PlanSyncTransferClearUseCase,
    private val settleSyncTransferCompletionsUseCase: SettleSyncTransferCompletionsUseCase,
) {
    operator fun invoke(
        state: SyncTransferStoreState,
        input: SyncTransferStoreInput,
        nowMillis: Long,
        trackedLimit: Int = SyncTransferStore.TRACKED_ROW_LIMIT,
        completedLimit: Int = SyncTransferStore.COMPLETED_ROW_LIMIT,
    ): SyncTransferStoreState {
        val fold = StoreFold(state, trackedLimit)
        when (input) {
            is SyncTransferStoreInput.Feed -> when (val update = input.update) {
                is SyncTransferUpdate.Seed ->
                    if (state.isSeeded) fold.reseed(update.entries, Long.MAX_VALUE) else fold.seed(update.entries)

                is SyncTransferUpdate.Batch ->
                    update.events.sortedBy { it.entry.notificationNumber }.forEach { fold.applyEvent(it) }

                is SyncTransferUpdate.Reseed -> fold.reseed(update.entries, update.watermark)
            }

            is SyncTransferStoreInput.ClearCompleted -> fold.clear(input.tags)
            SyncTransferStoreInput.ClearAllCompleted -> fold.clear(tags = null)
            is SyncTransferStoreInput.SetOptimisticPause -> input.tags.forEach { fold.applyPause(it, input.isPaused) }
            is SyncTransferStoreInput.RevertOptimisticPause -> fold.applyPause(input.tag, requestedPause = null)
        }
        return fold.commit(nowMillis, completedLimit)
    }

    /** Applies the steps the rules decide to one set of builders per input, then commits them once. */
    private inner class StoreFold(
        private val state: SyncTransferStoreState,
        private val trackedLimit: Int,
    ) {
        // Builders copy nothing until modified, so an input that changes nothing returns the same instances.
        private var isSeeded = state.isSeeded
        private val active = state.active.toPersistentHashMap().builder()
        private val overflow = state.overflow.toPersistentHashMap().builder()
        private val reseedRemoved = state.reseedRemoved.toPersistentHashSet().builder()
        private val reseedKeptCompleting = state.reseedKeptCompleting.toPersistentHashSet().builder()
        private var completed = state.completed.toPersistentList()
        private val journal = ArrayList<Emission>()

        fun seed(entries: List<SyncTransferEntry>) {
            isSeeded = true
            orderSyncTransferSeedUseCase(entries).forEach { admit(it, it.notificationNumber) }
        }

        fun applyEvent(event: SyncTransferEvent) {
            val entry = event.entry
            val tag = entry.tag
            val stored = active[tag]
            val parked = overflow[tag]
            when (val step = decideSyncTransferEventUseCase(event, stored, parked, tag in reseedRemoved)) {
                EventStep.Stale -> Unit
                is EventStep.Complete -> {
                    leave(tag, stored)
                    complete(entry, step.mediaType)
                }

                is EventStep.RollBack -> {
                    leave(tag, stored)
                    rollBack(step.direction, step.mediaType, entry.notificationNumber)
                }

                is EventStep.Merge -> {
                    reseedKeptCompleting.remove(tag)
                    active[tag] = step.row
                }

                EventStep.Place -> parked?.let { put(entry, it.mediaType, it) }
                is EventStep.AdmitAndComplete -> {
                    reseedRemoved.remove(tag)
                    val mediaType = getSyncMediaTypeUseCase(entry.fileName)
                    emit(SyncTransferTransition.Admitted(entry.direction, mediaType, step.startedAt))
                    complete(entry, mediaType)
                }

                EventStep.Ignore -> reseedRemoved.remove(tag)
                is EventStep.Admit -> {
                    reseedRemoved.remove(tag)
                    admit(entry, step.at)
                }
            }
        }

        fun reseed(entries: List<SyncTransferEntry>, watermark: Long) {
            val previousKept = state.reseedKeptCompleting
            reseedRemoved.clear()
            reseedKeptCompleting.clear()
            val completedTags = state.completed.mapTo(HashSet()) { it.entry.tag }
            val present = HashSet<Int>(entries.size * 2)
            for (entry in entries) {
                val tag = entry.tag
                val parked = overflow[tag]
                val step = decideSyncReseedEntryUseCase(entry, tag in completedTags, active[tag], parked)
                if (step == ReseedStep.Terminal) continue
                present += tag
                when (step) {
                    ReseedStep.Terminal, ReseedStep.Unchanged -> Unit
                    is ReseedStep.Merge -> active[tag] = step.row
                    ReseedStep.Place -> parked?.let { put(entry, it.mediaType, it) }
                    ReseedStep.Admit -> admit(entry, entry.notificationNumber)
                }
            }
            for ((tag, row) in state.active) {
                val entry = row.entry
                val step = decideSyncStaleTrackedRowUseCase(
                    entry.state, entry.notificationNumber, watermark, tag in present, tag in previousKept,
                )
                when (step) {
                    StaleStep.Untouched -> Unit
                    StaleStep.KeepCompleting -> reseedKeptCompleting += tag
                    StaleStep.Remove -> {
                        active.remove(tag)
                        rollBackReseeded(entry.direction, row.mediaType)
                        reseedRemoved += tag
                    }
                }
            }
            for ((tag, parked) in state.overflow) {
                if (tag in present) continue
                overflow.remove(tag)
                rollBackReseeded(parked.direction, parked.mediaType)
                reseedRemoved += tag
            }
        }

        fun clear(tags: Set<Int>?) {
            val plan = planSyncTransferClearUseCase(completed, tags, state.counts)
            plan.kept?.let { completed = it.toPersistentList() }
            plan.transitions.forEach(::emit)
        }

        fun applyPause(tag: Int, requestedPause: Boolean?) {
            val row = active[tag] ?: return
            decideOptimisticPauseUseCase(row, requestedPause)?.let { active[tag] = it }
        }

        fun commit(nowMillis: Long, completedLimit: Int): SyncTransferStoreState {
            val next = SyncTransferStoreState(
                isSeeded = isSeeded,
                active = active.build(),
                overflow = overflow.build(),
                completed = completed,
                counts = state.counts,
                reseedRemoved = reseedRemoved.build(),
                reseedKeptCompleting = reseedKeptCompleting.build(),
            )
            return settleSyncTransferCompletionsUseCase(next, journal, nowMillis, completedLimit)
        }

        private fun leave(tag: Int, stored: SyncTransferRow?) {
            if (stored != null) {
                reseedKeptCompleting.remove(tag)
                active.remove(tag)
            } else {
                overflow.remove(tag)
            }
        }

        private fun admit(entry: SyncTransferEntry, at: Long) {
            val mediaType = getSyncMediaTypeUseCase(entry.fileName)
            put(entry, mediaType)
            emit(SyncTransferTransition.Admitted(entry.direction, mediaType, at))
        }

        private fun put(entry: SyncTransferEntry, mediaType: SyncMediaType, parked: SyncTransferOverflowEntry? = null) {
            when (val placement = placeSyncTransferUseCase(entry, mediaType, parked, active.size, trackedLimit)) {
                Placement.Track -> {
                    if (parked != null) overflow.remove(entry.tag)
                    active[entry.tag] = SyncTransferRow(entry, mediaType, optimisticPause = null)
                }

                is Placement.Park -> overflow[entry.tag] = placement.entry
                Placement.Keep -> Unit
            }
        }

        private fun complete(entry: SyncTransferEntry, mediaType: SyncMediaType) {
            journal += Emission(
                SyncTransferTransition.Completed(entry.direction, entry.notificationNumber),
                Completion(entry, mediaType),
            )
        }

        private fun rollBack(direction: SyncTransferDirection, mediaType: SyncMediaType, at: Long) {
            emit(SyncTransferTransition.RolledBack(direction, mediaType, at))
        }

        private fun rollBackReseeded(direction: SyncTransferDirection, mediaType: SyncMediaType) {
            rollBack(direction, mediaType, DecideSyncStaleTrackedRowUseCase.RESEED_ROLL_BACK_ORDER)
        }

        private fun emit(transition: SyncTransferTransition) {
            journal += Emission(transition)
        }
    }
}
