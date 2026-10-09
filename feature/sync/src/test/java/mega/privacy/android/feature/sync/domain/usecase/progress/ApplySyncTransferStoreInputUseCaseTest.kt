package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferBatchProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEvent
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEventType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferUpdate
import mega.privacy.android.feature.sync.domain.entity.progress.batchProgress
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplySyncTransferStoreInputUseCaseTest {
    private lateinit var underTest: ApplySyncTransferStoreInputUseCase

    private val getFileTypeInfoByNameUseCase = mock<GetFileTypeInfoByNameUseCase>()

    @BeforeAll
    fun setUp() {
        val mergeSyncTransferRowUseCase = MergeSyncTransferRowUseCase()
        underTest = ApplySyncTransferStoreInputUseCase(
            getSyncMediaTypeUseCase = GetSyncMediaTypeUseCase(getFileTypeInfoByNameUseCase),
            placeSyncTransferUseCase = PlaceSyncTransferUseCase(),
            orderSyncTransferSeedUseCase = OrderSyncTransferSeedUseCase(),
            decideSyncTransferEventUseCase = DecideSyncTransferEventUseCase(mergeSyncTransferRowUseCase),
            decideSyncReseedEntryUseCase = DecideSyncReseedEntryUseCase(mergeSyncTransferRowUseCase),
            decideSyncStaleTrackedRowUseCase = DecideSyncStaleTrackedRowUseCase(),
            decideOptimisticPauseUseCase = DecideOptimisticPauseUseCase(),
            planSyncTransferClearUseCase = PlanSyncTransferClearUseCase(),
            settleSyncTransferCompletionsUseCase =
                SettleSyncTransferCompletionsUseCase(ApplySyncTransferCountersUseCase()),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFileTypeInfoByNameUseCase)
        whenever(getFileTypeInfoByNameUseCase(any(), any())).thenAnswer { invocation ->
            fileTypeInfoFor(invocation.getArgument<String>(0))
        }
    }

    @Test
    fun `test that invoke admits every seed entry and marks the store seeded`() {
        val state = seeded(entry(1), entry(2), entry(3))

        assertThat(state.isSeeded).isTrue()
        assertThat(state.active.keys).containsExactly(1, 2, 3)
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 3, pending = 3))
        assertThat(state.counts.batchUploads).isEqualTo(SyncTransferTally(total = 3, pending = 3))
    }

    @Test
    fun `test that invoke skips terminal entries in a seed`() {
        val state = seeded(
            entry(1, TransferState.STATE_COMPLETED),
            entry(2, TransferState.STATE_FAILED),
            entry(3, TransferState.STATE_CANCELLED),
            entry(4),
        )

        assertThat(state.active.keys).containsExactly(4)
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(1)
        assertThat(state.completed).isEmpty()
    }

    @Test
    fun `test that invoke keeps every slot holding seed entry beyond the tracked limit`() {
        val state = seeded(
            entry(1, TransferState.STATE_ACTIVE),
            entry(2, TransferState.STATE_RETRYING),
            entry(3, TransferState.STATE_COMPLETING),
            entry(4),
            entry(5),
            trackedLimit = 2,
        )

        assertThat(state.active.keys).containsExactly(1, 2, 3)
        assertThat(state.overflow.keys).containsExactly(4, 5)
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(5)
    }

    @Test
    fun `test that invoke tracks the lowest priority seed entries up to the tracked limit`() {
        val state = seeded(
            entry(1, priority = 50L),
            entry(2, priority = 10L),
            entry(3, priority = 40L),
            entry(4, priority = 20L),
            entry(5, priority = 30L),
            trackedLimit = 3,
        )

        assertThat(state.active.values.map { it.entry.priority }).containsExactly(10L, 20L, 30L)
        assertThat(state.overflow.keys).containsExactly(1, 3)
    }

    @Test
    fun `test that invoke admits slot holding seed entries only up to the slot holder headroom`() {
        val entries = (1..258).map { entry(it, TransferState.STATE_ACTIVE) }

        val state = seeded(*entries.toTypedArray(), trackedLimit = 1)

        assertThat(state.active.size).isEqualTo(257)
        assertThat(state.active.keys).containsExactlyElementsIn((1..257).toList())
        assertThat(state.overflow.keys).containsExactly(258)
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(258)
    }

    @ParameterizedTest
    @ValueSource(longs = [4L, 5L])
    fun `test that invoke ignores an event that is not newer than the stored row`(eventNumber: Long) {
        val before = seeded(entry(1, n = 5L))

        val after = before.applyAll(batch(update(entry(1, TransferState.STATE_ACTIVE, n = eventNumber))))

        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `test that invoke puts the newest completions first in the completed list`() {
        val seeded = seeded(entry(1), entry(2), entry(3))
        val firstCompletion = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            now = 1_000L,
        )

        val after = firstCompletion.applyAll(
            batch(
                finish(entry(3, TransferState.STATE_COMPLETED, n = 5L)),
                finish(entry(2, TransferState.STATE_COMPLETED, n = 4L)),
            ),
            now = 2_000L,
        )

        assertThat(after.completed.map { it.entry.tag }).containsExactly(3, 2, 1).inOrder()
        assertThat(after.completed.map { it.finishedAtMillis })
            .containsExactly(2_000L, 2_000L, 1_000L).inOrder()
    }

    @Test
    fun `test that invoke keeps the path and the batch id on a completed row`() {
        val state = seeded(entry(1)).applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L, path = "/sync/a.jpg"))),
        )

        assertThat(state.completed.single().entry.path).isEqualTo("/sync/a.jpg")
        assertThat(state.completed.single().batchId).isEqualTo(0L)
    }

    @Test
    fun `test that invoke treats an update carrying the completed state as a finish`() {
        val state = seeded(entry(1)).applyAll(
            batch(update(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
        )

        assertThat(state.active).isEmpty()
        assertThat(state.completed.map { it.entry.tag }).containsExactly(1)
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 0))
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_FAILED", "STATE_CANCELLED"])
    fun `test that invoke rolls back the counters when a transfer fails`(state: TransferState) {
        val after = seeded(entry(1)).applyAll(batch(finish(entry(1, state, n = 2L))))

        assertThat(after.active).isEmpty()
        assertThat(after.completed).isEmpty()
        assertThat(after.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 0, pending = 0))
        assertThat(after.counts.batchUploads).isEqualTo(SyncTransferTally(total = 0, pending = 0))
        assertThat(after.counts.byMediaType).isEmpty()
    }

    @Test
    fun `test that invoke removes a row silently when a finish reports another state`() {
        val after = seeded(entry(1)).applyAll(
            batch(finish(entry(1, TransferState.STATE_ACTIVE, n = 2L))),
        )

        assertThat(after.active).isEmpty()
        assertThat(after.completed).isEmpty()
        assertThat(after.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 0, pending = 0))
    }

    @ParameterizedTest
    @MethodSource("monotonicBytes")
    fun `test that invoke keeps the transferred bytes monotonic while the total size is unchanged`(
        storedTransferred: Long,
        storedTotal: Long,
        newTransferred: Long,
        newTotal: Long,
        expectedTransferred: Long,
    ) {
        val stored = entry(1, TransferState.STATE_ACTIVE, transferred = storedTransferred, total = storedTotal)
        val newer = entry(
            1,
            TransferState.STATE_ACTIVE,
            n = 2L,
            transferred = newTransferred,
            total = newTotal,
        ).copy(speed = 7L)

        val state = seeded(stored).applyAll(batch(update(newer)))

        assertThat(state.active.getValue(1).entry)
            .isEqualTo(newer.copy(transferredBytes = expectedTransferred))
    }

    @Test
    fun `test that invoke keeps the media type of a row across updates`() {
        val state = seeded(entry(1, fileName = "f1.jpg")).applyAll(
            batch(update(entry(1, TransferState.STATE_ACTIVE, n = 2L, fileName = "x.mp4"))),
        )

        assertThat(state.active.getValue(1).mediaType).isEqualTo(SyncMediaType.Image)
    }

    @ParameterizedTest
    @MethodSource("optimisticPauseMerges")
    fun `test that invoke resolves the optimistic pause as the merge table says when an update arrives`(
        startState: TransferState,
        optimisticBefore: Boolean?,
        updateState: TransferState,
        expected: Boolean?,
    ) {
        val seededState = seeded(entry(1, startState))
        val withOptimistic = optimisticBefore?.let { seededState.applyAll(setPause(it, 1)) } ?: seededState

        val after = withOptimistic.applyAll(batch(update(entry(1, updateState, n = 2L))))

        assertThat(after.active.getValue(1).optimisticPause).isEqualTo(expected)
    }

    @Test
    fun `test that invoke keeps an overflow tag counted until it finishes`() {
        val seeded = seeded(entry(1), entry(2), trackedLimit = 1)
        assertThat(seeded.overflow.keys).containsExactly(2)

        val updated = seeded.applyAll(batch(update(entry(2, TransferState.STATE_QUEUED, n = 2L))), trackedLimit = 1)
        assertThat(updated.overflow.keys).containsExactly(2)
        assertThat(updated.active).isEqualTo(seeded.active)

        val finished = updated.applyAll(
            batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 3L))),
            trackedLimit = 1,
        )
        assertThat(finished.counts.pendingSyncTransfers).isEqualTo(1)
        assertThat(finished.counts.completedTransfers).isEqualTo(1)
        assertThat(finished.overflow).isEmpty()
    }

    @Test
    fun `test that invoke moves a completed overflow tag to the completed list`() {
        val state = SyncTransferStoreState.NotSeeded.applyAll(
            seed(entry(1), entry(2, fileName = "f2.mp4")),
            batch(
                finish(
                    entry(2, TransferState.STATE_COMPLETED, n = 3L, path = "/sync/2.mp4", fileName = "x.jpg"),
                ),
            ),
            trackedLimit = 1,
        )

        assertThat(state.completed.single().entry.path).isEqualTo("/sync/2.mp4")
        assertThat(state.completed.single().mediaType).isEqualTo(SyncMediaType.Video)
    }

    @Test
    fun `test that invoke rolls back a failed overflow tag`() {
        val seeded = seeded(entry(1), entry(2, fileName = "f2.mp4"), trackedLimit = 1)

        val after = seeded.applyAll(
            batch(finish(entry(2, TransferState.STATE_FAILED, n = 2L, fileName = "f2.mp4"))),
            trackedLimit = 1,
        )

        assertThat(after.overflow).isEmpty()
        assertThat(after.counts.pendingSyncTransfers).isEqualTo(1)
        assertThat(after.counts.byMediaType).doesNotContainKey(SyncMediaType.Video)
    }

    @Test
    fun `test that invoke keeps the latest state of an overflow tag`() {
        val seeded = seeded(entry(1), entry(2), trackedLimit = 1)

        val after = seeded.applyAll(
            batch(update(entry(2, TransferState.STATE_PAUSED, n = 2L))),
            trackedLimit = 1,
        )

        assertThat(after.overflow.getValue(2).state).isEqualTo(TransferState.STATE_PAUSED)
    }

    @Test
    fun `test that invoke admits only slot holding states at the tracked limit`() {
        val seeded = seeded(entry(1), trackedLimit = 1)

        val afterPaused = seeded.applyAll(
            batch(start(entry(2, TransferState.STATE_PAUSED, n = 2L))),
            trackedLimit = 1,
        )
        val afterActive = afterPaused.applyAll(
            batch(start(entry(3, TransferState.STATE_ACTIVE, n = 3L))),
            trackedLimit = 1,
        )

        assertThat(afterPaused.overflow.keys).containsExactly(2)
        assertThat(afterActive.active.size).isEqualTo(2)
        assertThat(afterActive.overflow.keys).containsExactly(2)
    }

    @Test
    fun `test that invoke promotes an overflow tag when it takes a slot`() {
        val seeded = seeded(entry(1), entry(2), trackedLimit = 1)

        val after = seeded.applyAll(
            batch(update(entry(2, TransferState.STATE_ACTIVE, n = 2L))),
            trackedLimit = 1,
        )

        assertThat(after.active.containsKey(2)).isTrue()
        assertThat(after.overflow).isEmpty()
        assertThat(after.active.getValue(2).optimisticPause).isNull()
    }

    @Test
    fun `test that invoke promotes an overflow tag when the tracked rows fall below the limit`() {
        val seeded = seeded(entry(1), entry(2), trackedLimit = 1)

        val afterFinish = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            trackedLimit = 1,
        )
        val after = afterFinish.applyAll(
            batch(update(entry(2, TransferState.STATE_QUEUED, n = 3L))),
            trackedLimit = 1,
        )

        assertThat(after.active.containsKey(2)).isTrue()
    }

    @Test
    fun `test that invoke keeps a slot holding tag in overflow when the slot holder headroom is full`() {
        val slotHolders = (1..257).map { entry(it, TransferState.STATE_ACTIVE) }
        val seeded = seeded(*slotHolders.toTypedArray(), entry(900), trackedLimit = 1)
        assertThat(seeded.active.size).isEqualTo(257)
        assertThat(seeded.overflow.keys).containsExactly(900)

        val activeUpdate = seeded.applyAll(
            batch(update(entry(900, TransferState.STATE_ACTIVE, n = 2L))),
            trackedLimit = 1,
        )
        assertThat(activeUpdate.overflow.getValue(900).state).isEqualTo(TransferState.STATE_ACTIVE)
        assertThat(activeUpdate.active.containsKey(900)).isFalse()

        val unknownStart = activeUpdate.applyAll(
            batch(start(entry(901, TransferState.STATE_ACTIVE, n = 3L))),
            trackedLimit = 1,
        )
        assertThat(unknownStart.overflow.keys).containsExactly(900, 901)

        val rowFinished = unknownStart.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 4L))),
            trackedLimit = 1,
        )
        val promoted = rowFinished.applyAll(
            batch(update(entry(900, TransferState.STATE_ACTIVE, n = 5L))),
            trackedLimit = 1,
        )
        assertThat(promoted.active.size).isEqualTo(257)
        assertThat(promoted.active.containsKey(900)).isTrue()
    }

    @Test
    fun `test that invoke never demotes a tracked row above the tracked limit`() {
        val seeded = seeded(
            entry(1, TransferState.STATE_ACTIVE),
            entry(2, TransferState.STATE_ACTIVE),
            trackedLimit = 1,
        )

        val after = seeded.applyAll(
            batch(
                update(entry(1, TransferState.STATE_QUEUED, n = 2L)),
                update(entry(2, TransferState.STATE_QUEUED, n = 3L)),
            ),
            trackedLimit = 1,
        )

        assertThat(after.active.keys).containsExactly(1, 2)
        assertThat(after.overflow).isEmpty()
    }

    @Test
    fun `test that invoke admits an update for an unknown tag as a start`() {
        val state = SyncTransferStoreState.NotSeeded.applyAll(batch(update(entry(7))))

        assertThat(state.active.containsKey(7)).isTrue()
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
    }

    @Test
    fun `test that invoke keeps the batch open when a start precedes the last finish in one batch`() {
        val seeded = seeded(entry(2, n = 5L))

        val state = seeded.applyAll(
            batch(
                finish(entry(2, TransferState.STATE_COMPLETED, n = 7L)),
                update(entry(1, TransferState.STATE_ACTIVE, n = 8L), startN = 6L),
            ),
        )

        assertThat(state.counts.batchId).isEqualTo(0L)
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 2, pending = 1))
        assertThat(state.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 2, total = 2))
        assertThat(state.completed.single().batchId).isEqualTo(0L)
    }

    @Test
    fun `test that invoke counts a start and finish in one batch as completed`() {
        val state = SyncTransferStoreState.NotSeeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 3L), startN = 1L)),
        )

        assertThat(state.completed.size).isEqualTo(1)
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 0))
    }

    @ParameterizedTest
    @EnumSource(names = ["STATE_COMPLETED", "STATE_FAILED", "STATE_CANCELLED"])
    fun `test that invoke ignores a terminal event for a tag it never saw`(state: TransferState) {
        val before = SyncTransferStoreState.NotSeeded

        val after = before.applyAll(batch(finish(entry(1, state, n = 1L))))

        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `test that invoke gives each completed row the batch it finished in`() {
        val seeded = seeded(entry(1))

        val state = seeded.applyAll(
            batch(
                finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)),
                finish(entry(2, TransferState.STATE_COMPLETED, n = 4L), startN = 3L),
            ),
        )

        assertThat(state.completed.first { it.entry.tag == 1 }.batchId).isEqualTo(0L)
        assertThat(state.completed.first { it.entry.tag == 2 }.batchId).isEqualTo(1L)
        assertThat(state.counts.batchId).isEqualTo(2L)
    }

    @Test
    fun `test that invoke keeps completed rows and resets the batch when every pending row is cancelled`() {
        val seeded = seeded(entry(1), entry(2), entry(3), entry(9))
        val withCompleted = seeded.applyAll(batch(finish(entry(9, TransferState.STATE_COMPLETED, n = 2L))))

        val state = withCompleted.applyAll(
            batch(
                finish(entry(1, TransferState.STATE_CANCELLED, n = 3L)),
                finish(entry(2, TransferState.STATE_CANCELLED, n = 4L)),
                finish(entry(3, TransferState.STATE_CANCELLED, n = 5L)),
            ),
        )

        assertThat(state.active).isEmpty()
        assertThat(state.completed.map { it.entry.tag }).containsExactly(9)
        assertThat(state.counts.batchProgress()).isNull()
        assertThat(state.counts.batchId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke evicts the oldest completed row beyond the completed limit`() {
        val seeded = seeded(entry(1), entry(2), entry(3))

        val state = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 3L))),
            batch(finish(entry(3, TransferState.STATE_COMPLETED, n = 4L))),
            completedLimit = 2,
        )

        assertThat(state.completed.map { it.entry.tag }).containsExactly(3, 2).inOrder()
    }

    @Test
    fun `test that invoke lowers only the lifetime counts when a completed row is evicted`() {
        val seeded = seeded(entry(1), entry(2), entry(3))

        val state = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 3L))),
            completedLimit = 1,
        )

        assertThat(state.counts.batchUploads).isEqualTo(SyncTransferTally(total = 3, pending = 1))
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 2, pending = 1))
        assertThat(state.counts.byMediaType[SyncMediaType.Image]).isEqualTo(2)
    }

    @Test
    fun `test that invoke tallies an evicted completion only when it finished in the current batch`() {
        val seeded = seeded(entry(1), completedLimit = 1)
        val firstBatchDone = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            completedLimit = 1,
        )
        assertThat(firstBatchDone.counts.batchId).isEqualTo(1L)

        val started = firstBatchDone.applyAll(
            batch(
                start(entry(2, n = 3L)),
                start(entry(3, n = 4L)),
                start(entry(4, n = 5L)),
            ),
            completedLimit = 1,
        )
        val secondDone = started.applyAll(
            batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 6L))),
            completedLimit = 1,
        )
        assertThat(secondDone.counts.batchEvictedUploads).isEqualTo(0)

        val thirdDone = secondDone.applyAll(
            batch(finish(entry(3, TransferState.STATE_COMPLETED, n = 7L))),
            completedLimit = 1,
        )
        assertThat(thirdDone.counts.batchEvictedUploads).isEqualTo(1)
    }

    @Test
    fun `test that invoke clears one completed row and its counts`() {
        val seeded = seeded(entry(1), entry(2))

        val state = seeded.applyAll(
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
            SyncTransferStoreInput.ClearCompleted(setOf(1)),
        )

        assertThat(state.completed).isEmpty()
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
        assertThat(state.counts.byMediaType[SyncMediaType.Image]).isEqualTo(1)
    }

    @Test
    fun `test that invoke clears every completed row and their counts`() {
        val seeded = seeded(entry(1), entry(2), entry(3))

        val state = seeded.applyAll(
            batch(
                finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)),
                finish(entry(2, TransferState.STATE_COMPLETED, n = 3L)),
                finish(entry(3, TransferState.STATE_COMPLETED, n = 4L)),
            ),
            SyncTransferStoreInput.ClearAllCompleted,
        )

        assertThat(state.completed).isEmpty()
        assertThat(state.counts.completedTransfers).isEqualTo(0)
    }

    @Test
    fun `test that invoke lowers the batch when a cleared row finished in the current batch`() {
        val seeded = seeded(entry(1), entry(2), entry(3))
        val twoDone = seeded.applyAll(
            batch(
                finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)),
                finish(entry(2, TransferState.STATE_COMPLETED, n = 3L)),
            ),
        )
        assertThat(twoDone.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 3, total = 3))

        val cleared = twoDone.applyAll(SyncTransferStoreInput.ClearCompleted(setOf(1, 2)))

        assertThat(cleared.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 1, total = 1))
    }

    @Test
    fun `test that invoke leaves the batch when a cleared row finished in an older batch`() {
        val seeded = seeded(entry(1))
        val oldBatchDone = seeded.applyAll(batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))))
        val newBatchStarted = oldBatchDone.applyAll(batch(start(entry(2, n = 3L))))
        val batchBefore = newBatchStarted.counts.batchUploads

        val state = newBatchStarted.applyAll(SyncTransferStoreInput.ClearCompleted(setOf(1)))

        assertThat(state.counts.batchUploads).isEqualTo(batchBefore)
        assertThat(state.counts.batchId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke lowers the batch by the evicted completions when every completed row is cleared`() {
        val beforeClear = evictedCompletionsBase()
        assertThat(beforeClear.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 4, total = 4))
        assertThat(beforeClear.counts.batchEvictedUploads).isEqualTo(2)

        val cleared = beforeClear.applyAll(SyncTransferStoreInput.ClearAllCompleted, completedLimit = 1)

        assertThat(cleared.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 1, total = 1))
        assertThat(cleared.counts.batchEvictedUploads).isEqualTo(0)
        assertThat(cleared.completed).isEmpty()
    }

    @Test
    fun `test that invoke keeps the evicted tally when only some completed rows are cleared`() {
        val beforeClear = evictedCompletionsBase()

        val cleared = beforeClear.applyAll(
            SyncTransferStoreInput.ClearCompleted(setOf(4)),
            completedLimit = 1,
        )

        assertThat(cleared.counts.batchEvictedUploads).isEqualTo(2)
        assertThat(cleared.counts.batchProgress()).isEqualTo(SyncTransferBatchProgress(current = 3, total = 3))
    }

    @ParameterizedTest
    @MethodSource("optimisticPauseCommands")
    fun `test that invoke sets the optimistic state as the pause table says`(
        state: TransferState,
        optimisticBefore: Boolean?,
        isPaused: Boolean,
        expected: Boolean?,
    ) {
        val seededState = seeded(entry(1, state))
        val withOptimistic = optimisticBefore?.let { seededState.applyAll(setPause(it, 1)) } ?: seededState

        val after = withOptimistic.applyAll(setPause(isPaused, 1))

        assertThat(after.active.getValue(1).optimisticPause).isEqualTo(expected)
    }

    @Test
    fun `test that invoke keeps an optimistic resume over a pause still in flight`() {
        val seeded = seeded(entry(1, TransferState.STATE_ACTIVE))
        val paused = seeded.applyAll(setPause(true, 1))
        val resumed = paused.applyAll(setPause(false, 1))
        assertThat(resumed.active.getValue(1).optimisticPause).isEqualTo(false)

        val pausedEcho = resumed.applyAll(batch(update(entry(1, TransferState.STATE_PAUSED, n = 2L))))
        assertThat(pausedEcho.active.getValue(1).optimisticPause).isEqualTo(false)

        val queuedEcho = pausedEcho.applyAll(batch(update(entry(1, TransferState.STATE_QUEUED, n = 3L))))
        assertThat(queuedEcho.active.getValue(1).optimisticPause).isNull()
    }

    @Test
    fun `test that invoke ignores an optimistic pause for a completing row`() {
        val before = seeded(entry(1, TransferState.STATE_COMPLETING))

        val after = before.applyAll(setPause(true, 1))

        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `test that invoke reverts the optimistic pause of a row`() {
        val seeded = seeded(entry(1, TransferState.STATE_ACTIVE))

        val after = seeded.applyAll(
            setPause(true, 1),
            SyncTransferStoreInput.RevertOptimisticPause(1),
        )

        assertThat(after.active.getValue(1).optimisticPause).isNull()
    }

    @ParameterizedTest
    @MethodSource("commandsThatMatchNothing")
    fun `test that invoke returns an equal state for a command that matches nothing`(
        command: SyncTransferStoreInput,
        useEmptyBase: Boolean,
    ) {
        val before = if (useEmptyBase) SyncTransferStoreState.NotSeeded else matchNothingBase()

        val after = before.applyAll(command, trackedLimit = 1)

        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `test that invoke returns the same collections for inputs that change nothing`() {
        val before = matchNothingBase()

        val after = before.applyAll(
            batch(update(entry(1, TransferState.STATE_ACTIVE, n = 1L))),
            SyncTransferStoreInput.ClearCompleted(setOf(99)),
            trackedLimit = 1,
        )

        assertThat(after.active).isSameInstanceAs(before.active)
        assertThat(after.overflow).isSameInstanceAs(before.overflow)
        assertThat(after.completed).isSameInstanceAs(before.completed)
    }

    @Test
    fun `test that invoke applies the events of a batch in notification number order`() {
        val seeded = seeded(entry(1), trackedLimit = 1)

        val after = seeded.applyAll(
            batch(
                start(entry(2, TransferState.STATE_QUEUED, n = 9L)),
                finish(entry(1, TransferState.STATE_COMPLETED, n = 8L)),
            ),
            trackedLimit = 1,
        )

        assertThat(after.active.containsKey(2)).isTrue()
        assertThat(after.overflow).isEmpty()
    }

    @Test
    fun `test that invoke reseed keeps a row newer than its entry`() {
        val before = seeded(entry(1, n = 9L, transferred = 50L))

        val after = before.applyAll(reseed(100L, entry(1, TransferState.STATE_ACTIVE, n = 7L, transferred = 20L)))

        assertThat(after.active.getValue(1)).isEqualTo(before.active.getValue(1))
    }

    @Test
    fun `test that invoke reseed replaces a row older than its entry`() {
        val seeded = seeded(entry(1, TransferState.STATE_QUEUED, n = 7L))

        val after = seeded.applyAll(
            reseed(100L, entry(1, TransferState.STATE_ACTIVE, n = 9L, transferred = 50L, total = 100L)),
        )

        assertThat(after.active.getValue(1).entry.transferredBytes).isEqualTo(50L)
        assertThat(after.active.getValue(1).entry.state).isEqualTo(TransferState.STATE_ACTIVE)
    }

    @Test
    fun `test that invoke reseed skips a tag already completed`() {
        val withCompleted = seeded(entry(1)).applyAll(batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))))

        val after = withCompleted.applyAll(reseed(100L, entry(1, TransferState.STATE_QUEUED, n = 3L)))

        assertThat(after.active).isEmpty()
        assertThat(after.counts).isEqualTo(withCompleted.counts)
    }

    @Test
    fun `test that invoke reseed admits an unknown entry and skips a terminal one`() {
        val state = SyncTransferStoreState.NotSeeded.applyAll(
            reseed(100L, entry(7), entry(8, TransferState.STATE_FAILED)),
        )

        assertThat(state.active.keys).containsExactly(7)
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(1)
    }

    @Test
    fun `test that invoke reseed admits a repeated tag once and merges its later entry`() {
        val state = seeded().applyAll(
            reseed(10L, entry(7, n = 5L), entry(7, TransferState.STATE_ACTIVE, n = 6L, transferred = 50L)),
        )

        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
        assertThat(state.active.getValue(7).entry.state).isEqualTo(TransferState.STATE_ACTIVE)
        assertThat(state.active.getValue(7).entry.transferredBytes).isEqualTo(50L)
    }

    @Test
    fun `test that invoke reseed removes an absent row at or below the watermark`() {
        val seeded = seeded(entry(1, n = 4L))

        val state = seeded.applyAll(reseed(5L))

        assertThat(state.active).isEmpty()
        assertThat(state.reseedRemoved).containsExactly(1)
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(0)
    }

    @Test
    fun `test that invoke reseed keeps an absent row above the watermark`() {
        val seeded = seeded(entry(1, n = 6L))

        val state = seeded.applyAll(reseed(5L))

        assertThat(state.active.containsKey(1)).isTrue()
        assertThat(state.reseedRemoved).isEmpty()
    }

    @Test
    fun `test that invoke reseed removes an absent overflow tag`() {
        val seeded = seeded(entry(1), entry(2, fileName = "f2.mp4"), trackedLimit = 1)

        val state = seeded.applyAll(reseed(100L, entry(1)), trackedLimit = 1)

        assertThat(state.overflow).isEmpty()
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(1)
        assertThat(state.counts.byMediaType).doesNotContainKey(SyncMediaType.Video)
        assertThat(state.reseedRemoved).containsExactly(2)
    }

    @Test
    fun `test that invoke reseed promotes an overflow tag listed in a slot holding state`() {
        val seeded = seeded(entry(1), entry(2), trackedLimit = 1)

        val state = seeded.applyAll(
            reseed(
                100L,
                entry(1, TransferState.STATE_QUEUED, n = 1L),
                entry(2, TransferState.STATE_ACTIVE, n = 3L),
            ),
            trackedLimit = 1,
        )

        assertThat(state.active.containsKey(2)).isTrue()
        assertThat(state.overflow).isEmpty()
    }

    @Test
    fun `test that invoke reseed returns an equal state when nothing needs reconciling`() {
        val before = seeded(entry(1), entry(2))

        val after = before.applyAll(reseed(100L, entry(1), entry(2)))

        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `test that invoke reseed keeps an absent completing row once`() {
        val before = seeded(entry(1, TransferState.STATE_COMPLETING))

        val after = before.applyAll(reseed(5L))

        assertThat(after.active.containsKey(1)).isTrue()
        assertThat(after.reseedKeptCompleting).containsExactly(1)
        assertThat(after).isNotEqualTo(before)
    }

    @Test
    fun `test that invoke reseed removes an absent completing row at the second reseed`() {
        val seeded = seeded(entry(1, TransferState.STATE_COMPLETING))

        val state = seeded.applyAll(reseed(5L), reseed(5L))

        assertThat(state.active).isEmpty()
        assertThat(state.reseedRemoved).containsExactly(1)
    }

    @Test
    fun `test that invoke reseed keeps a completing row that got an event in between`() {
        val seeded = seeded(entry(1, TransferState.STATE_COMPLETING))

        val state = seeded.applyAll(
            reseed(5L),
            batch(update(entry(1, TransferState.STATE_COMPLETING, n = 6L))),
            reseed(7L),
        )

        assertThat(state.active.containsKey(1)).isTrue()
        assertThat(state.reseedKeptCompleting).containsExactly(1)
    }

    @Test
    fun `test that invoke reseed removes an absent completing row at the second reseed after a stale event`() {
        val kept = seeded(entry(1, TransferState.STATE_COMPLETING, n = 5L)).applyAll(reseed(5L))

        val state = kept.applyAll(
            batch(update(entry(1, TransferState.STATE_COMPLETING, n = 4L))),
            reseed(7L),
        )

        assertThat(state.active).isEmpty()
        assertThat(state.reseedRemoved).containsExactly(1)
    }

    @Test
    fun `test that invoke counts a finish for a reseeded away tag as completed`() {
        val seeded = seeded(entry(1, n = 1L))

        val state = seeded.applyAll(
            reseed(5L),
            batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 6L))),
        )

        assertThat(state.completed.map { it.entry.tag }).containsExactly(1)
        assertThat(state.counts.completedTransfers).isEqualTo(1)
        assertThat(state.reseedRemoved).isEmpty()
    }

    @Test
    fun `test that invoke ignores a failed finish for a reseeded away tag`() {
        val seeded = seeded(entry(1, n = 1L))
        val afterReseed = seeded.applyAll(reseed(5L))

        val after = afterReseed.applyAll(batch(finish(entry(1, TransferState.STATE_FAILED, n = 6L))))

        assertThat(after.counts).isEqualTo(afterReseed.counts)
        assertThat(after.completed).isEmpty()
        assertThat(after.reseedRemoved).isEmpty()
    }

    @Test
    fun `test that invoke reseed leaves completed rows and finished counts untouched`() {
        val withCompleted = seeded(entry(1), entry(2), entry(3)).applyAll(
            batch(
                finish(entry(1, TransferState.STATE_COMPLETED, n = 2L)),
                finish(entry(2, TransferState.STATE_COMPLETED, n = 3L)),
            ),
        )

        val after = withCompleted.applyAll(reseed(5L))

        assertThat(after.active).isEmpty()
        assertThat(after.completed).isEqualTo(withCompleted.completed)
        assertThat(after.counts.completedTransfers).isEqualTo(withCompleted.counts.completedTransfers)
        assertThat(after.isSeeded).isTrue()
    }

    @Test
    fun `test that invoke treats a second seed in one generation as a reseed`() {
        val withCompleted = SyncTransferStoreState.NotSeeded.applyAll(
            seed(entry(1), entry(2), entry(5)),
            batch(finish(entry(5, TransferState.STATE_COMPLETED, n = 2L))),
        )

        val state = withCompleted.applyAll(seed(entry(2), entry(3)))

        assertThat(state.reseedRemoved).containsExactly(1)
        assertThat(state.active.keys).containsExactly(2, 3)
        assertThat(state.completed.map { it.entry.tag }).containsExactly(5)
    }

    @Test
    fun `test that invoke rolls back reseed removals before its admissions`() {
        val seeded = seeded(entry(1, n = 4L))

        val state = seeded.applyAll(reseed(5L, entry(2, n = 5L)))

        assertThat(state.counts.batchId).isEqualTo(1L)
        assertThat(state.counts.batchUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
        assertThat(state.counts.lifetimeUploads).isEqualTo(SyncTransferTally(total = 1, pending = 1))
    }

    @ParameterizedTest
    @ValueSource(ints = [7, 11, 13])
    fun `test that invoke keeps the counts consistent with the rows for any input sequence`(randomSeed: Int) {
        val generator = SyncInputGenerator(randomSeed)
        var state = SyncTransferStoreState.NotSeeded
        repeat(INPUT_COUNT + 1) { index ->
            val input = if (index == 0) generator.seedInput() else generator.next(state)
            state = underTest(
                state,
                input,
                nowMillis = index.toLong(),
                trackedLimit = RANDOM_TRACKED_LIMIT,
                completedLimit = RANDOM_COMPLETED_LIMIT,
            )
            assertInvariants(state, "input #$index: $input")
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `test that invoke folds ten thousand starts in one input`() {
        val starts = (1..BURST_SIZE).map { start(entry(it, TransferState.STATE_QUEUED, n = it.toLong())) }

        val state = SyncTransferStoreState.NotSeeded.applyAll(batchOf(starts))

        assertThat(state.active.size).isEqualTo(BURST_SIZE)
        assertThat(state.counts.lifetimeUploads)
            .isEqualTo(SyncTransferTally(total = BURST_SIZE, pending = BURST_SIZE))
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `test that invoke folds ten thousand cancellations in one input`() {
        val seeded = SyncTransferStoreState.NotSeeded.applyAll(
            seedOf((1..BURST_SIZE).map { entry(it, n = 1L) }),
        )
        val cancellations = (1..BURST_SIZE).map {
            finish(entry(it, TransferState.STATE_CANCELLED, n = 2L))
        }

        val state = seeded.applyAll(batchOf(cancellations))

        assertThat(state.active).isEmpty()
        assertThat(state.counts.pendingSyncTransfers).isEqualTo(0)
        assertThat(state.counts.batchId).isEqualTo(1L)
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `test that invoke folds ten thousand completions in one input`() {
        val seeded = SyncTransferStoreState.NotSeeded.applyAll(
            seedOf((1..BURST_SIZE).map { entry(it, n = 1L) }),
        )
        val completions = (1..BURST_SIZE).map {
            finish(entry(it, TransferState.STATE_COMPLETED, n = it + 1L))
        }

        val state = seeded.applyAll(batchOf(completions))

        assertThat(state.completed.size).isEqualTo(SyncTransferStore.COMPLETED_ROW_LIMIT)
        assertThat(state.completed.first().entry.tag).isEqualTo(BURST_SIZE)
        assertThat(state.counts.completedTransfers).isEqualTo(SyncTransferStore.COMPLETED_ROW_LIMIT)
        assertThat(state.counts.batchId).isEqualTo(1L)
        assertThat(state.counts.batchEvictedUploads).isEqualTo(0)
    }

    private fun assertInvariants(
        state: SyncTransferStoreState,
        label: String,
        trackedLimit: Int = RANDOM_TRACKED_LIMIT,
        completedLimit: Int = RANDOM_COMPLETED_LIMIT,
    ) {
        val counts = state.counts
        val completedTags = state.completed.map { it.entry.tag }
        val mediaTypes = state.active.values.map { it.mediaType } +
                state.overflow.values.map { it.mediaType } +
                state.completed.map { it.mediaType }

        assertWithMessage("$label: lifetime uploads pending").that(counts.lifetimeUploads.pending)
            .isEqualTo(
                state.active.values.count { it.entry.direction == SyncTransferDirection.Upload } +
                        state.overflow.values.count { it.direction == SyncTransferDirection.Upload }
            )
        assertWithMessage("$label: lifetime downloads pending").that(counts.lifetimeDownloads.pending)
            .isEqualTo(
                state.active.values.count { it.entry.direction == SyncTransferDirection.Download } +
                        state.overflow.values.count { it.direction == SyncTransferDirection.Download }
            )
        assertWithMessage("$label: pending transfers").that(counts.pendingSyncTransfers)
            .isEqualTo(state.active.size + state.overflow.size)
        assertWithMessage("$label: batch uploads pending").that(counts.batchUploads.pending)
            .isEqualTo(counts.lifetimeUploads.pending)
        assertWithMessage("$label: batch downloads pending").that(counts.batchDownloads.pending)
            .isEqualTo(counts.lifetimeDownloads.pending)
        assertWithMessage("$label: batch uploads completed").that(counts.batchUploads.completed)
            .isEqualTo(
                state.completed.count {
                    it.entry.direction == SyncTransferDirection.Upload && it.batchId == counts.batchId
                } + counts.batchEvictedUploads
            )
        assertWithMessage("$label: batch downloads completed").that(counts.batchDownloads.completed)
            .isEqualTo(
                state.completed.count {
                    it.entry.direction == SyncTransferDirection.Download && it.batchId == counts.batchId
                } + counts.batchEvictedDownloads
            )
        assertWithMessage("$label: completed transfers").that(counts.completedTransfers)
            .isEqualTo(state.completed.size)
        assertWithMessage("$label: media counts").that(counts.byMediaType)
            .isEqualTo(mediaTypes.groupingBy { it }.eachCount())
        assertWithMessage("$label: completed bound").that(state.completed.size).isAtMost(completedLimit)
        assertWithMessage("$label: completed tags distinct").that(completedTags.toSet().size)
            .isEqualTo(completedTags.size)
        assertWithMessage("$label: active bound")
            .that(state.active.size).isAtMost(trackedLimit + SyncTransferStore.SLOT_HOLDER_HEADROOM)
        assertWithMessage("$label: active and overflow disjoint")
            .that(state.active.keys.intersect(state.overflow.keys)).isEmpty()
        assertWithMessage("$label: active and completed disjoint")
            .that(state.active.keys.intersect(completedTags.toSet())).isEmpty()
        assertWithMessage("$label: overflow and completed disjoint")
            .that(state.overflow.keys.intersect(completedTags.toSet())).isEmpty()
        assertWithMessage("$label: reseed removed disjoint")
            .that(state.reseedRemoved.intersect(state.active.keys + state.overflow.keys)).isEmpty()
        assertWithMessage("$label: reseed kept completing tracked")
            .that(state.active.keys.containsAll(state.reseedKeptCompleting)).isTrue()
    }

    private fun matchNothingBase(): SyncTransferStoreState = SyncTransferStoreState.NotSeeded.applyAll(
        seed(entry(1), entry(2)),
        batch(finish(entry(3, TransferState.STATE_COMPLETED, n = 2L), startN = 1L)),
        trackedLimit = 1,
    )

    private fun evictedCompletionsBase(): SyncTransferStoreState = seeded(
        entry(1),
        entry(2),
        entry(3),
        entry(4, direction = SyncTransferDirection.Download),
        completedLimit = 1,
    ).applyAll(
        batch(finish(entry(1, TransferState.STATE_COMPLETED, n = 2L))),
        batch(finish(entry(2, TransferState.STATE_COMPLETED, n = 3L))),
        batch(finish(entry(4, TransferState.STATE_COMPLETED, n = 4L, direction = SyncTransferDirection.Download))),
        completedLimit = 1,
    )

    private fun SyncTransferStoreState.applyAll(
        vararg inputs: SyncTransferStoreInput,
        now: Long = 0L,
        trackedLimit: Int = SyncTransferStore.TRACKED_ROW_LIMIT,
        completedLimit: Int = SyncTransferStore.COMPLETED_ROW_LIMIT,
    ): SyncTransferStoreState = inputs.fold(this) { current, input ->
        underTest(current, input, now, trackedLimit, completedLimit)
    }

    private fun seeded(
        vararg entries: SyncTransferEntry,
        trackedLimit: Int = SyncTransferStore.TRACKED_ROW_LIMIT,
        completedLimit: Int = SyncTransferStore.COMPLETED_ROW_LIMIT,
    ): SyncTransferStoreState = SyncTransferStoreState.NotSeeded.applyAll(
        seed(*entries),
        trackedLimit = trackedLimit,
        completedLimit = completedLimit,
    )

    private fun start(entry: SyncTransferEntry, startN: Long? = null): SyncTransferEvent =
        SyncTransferEvent(SyncTransferEventType.Start, entry, startN)

    private fun update(entry: SyncTransferEntry, startN: Long? = null): SyncTransferEvent =
        SyncTransferEvent(SyncTransferEventType.Update, entry, startN)

    private fun finish(entry: SyncTransferEntry, startN: Long? = null): SyncTransferEvent = SyncTransferEvent(
        SyncTransferEventType.Finish,
        entry.copy(path = entry.path ?: "/sync/f${entry.tag}.jpg"),
        startN,
    )

    private fun batch(vararg events: SyncTransferEvent): SyncTransferStoreInput = batchOf(events.toList())

    private fun batchOf(events: List<SyncTransferEvent>): SyncTransferStoreInput =
        SyncTransferStoreInput.Feed(SyncTransferUpdate.Batch(events))

    private fun seed(vararg entries: SyncTransferEntry): SyncTransferStoreInput = seedOf(entries.toList())

    private fun seedOf(entries: List<SyncTransferEntry>): SyncTransferStoreInput =
        SyncTransferStoreInput.Feed(SyncTransferUpdate.Seed(entries))

    private fun reseed(watermark: Long, vararg entries: SyncTransferEntry): SyncTransferStoreInput =
        SyncTransferStoreInput.Feed(SyncTransferUpdate.Reseed(entries.toList(), watermark))

    private fun setPause(isPaused: Boolean, vararg tags: Int): SyncTransferStoreInput =
        SyncTransferStoreInput.SetOptimisticPause(tags.toSet(), isPaused)

    private fun fileTypeInfoFor(fileName: String): FileTypeInfo {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when (extension) {
            "jpg" -> StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
            "mp4" -> VideoFileTypeInfo(mimeType = "video/mp4", extension = "mp4", duration = Duration.ZERO)
            "mp3" -> AudioFileTypeInfo(mimeType = "audio/mpeg", extension = "mp3", duration = Duration.ZERO)
            "pdf" -> PdfFileTypeInfo
            "zip" -> ZipFileTypeInfo(mimeType = "application/zip", extension = "zip")
            else -> UnknownFileTypeInfo(mimeType = "application/octet-stream", extension = extension)
        }
    }

    private fun monotonicBytes(): Stream<Arguments> = Stream.of(
        Arguments.of(60L, 100L, 40L, 100L, 60L),
        Arguments.of(60L, 100L, 80L, 100L, 80L),
        Arguments.of(60L, 100L, 10L, 200L, 10L),
    )

    private fun optimisticPauseMerges(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_ACTIVE, true, TransferState.STATE_PAUSED, null),
        Arguments.of(TransferState.STATE_ACTIVE, true, TransferState.STATE_COMPLETING, null),
        Arguments.of(TransferState.STATE_ACTIVE, true, TransferState.STATE_ACTIVE, true),
        Arguments.of(TransferState.STATE_ACTIVE, true, TransferState.STATE_QUEUED, true),
        Arguments.of(TransferState.STATE_PAUSED, false, TransferState.STATE_QUEUED, null),
        Arguments.of(TransferState.STATE_PAUSED, false, TransferState.STATE_ACTIVE, null),
        Arguments.of(TransferState.STATE_PAUSED, false, TransferState.STATE_PAUSED, false),
        Arguments.of(TransferState.STATE_PAUSED, null, TransferState.STATE_PAUSED, null),
    )

    private fun optimisticPauseCommands(): Stream<Arguments> = Stream.of(
        Arguments.of(TransferState.STATE_PAUSED, null, true, null),
        Arguments.of(TransferState.STATE_QUEUED, null, false, null),
        Arguments.of(TransferState.STATE_ACTIVE, null, true, true),
        Arguments.of(TransferState.STATE_PAUSED, null, false, false),
        Arguments.of(TransferState.STATE_ACTIVE, true, false, false),
        Arguments.of(TransferState.STATE_PAUSED, false, true, true),
        Arguments.of(TransferState.STATE_ACTIVE, true, true, true),
    )

    private fun commandsThatMatchNothing(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncTransferStoreInput.ClearCompleted(setOf(99)), false),
        Arguments.of(SyncTransferStoreInput.ClearAllCompleted, true),
        Arguments.of(SyncTransferStoreInput.SetOptimisticPause(setOf(2), true), false),
        Arguments.of(SyncTransferStoreInput.SetOptimisticPause(setOf(99), true), false),
        Arguments.of(SyncTransferStoreInput.RevertOptimisticPause(99), false),
        Arguments.of(SyncTransferStoreInput.RevertOptimisticPause(1), false),
    )

    private companion object {
        const val RANDOM_TRACKED_LIMIT = 5
        const val RANDOM_COMPLETED_LIMIT = 3
        const val INPUT_COUNT = 2_000
        const val BURST_SIZE = 10_000
    }
}

private fun entry(
    tag: Int,
    state: TransferState = TransferState.STATE_QUEUED,
    n: Long = 1L,
    direction: SyncTransferDirection = SyncTransferDirection.Upload,
    transferred: Long = 0L,
    total: Long = 100L,
    priority: Long = tag.toLong(),
    fileName: String = "f$tag.jpg",
    path: String? = null,
): SyncTransferEntry = SyncTransferEntry(
    tag = tag,
    direction = direction,
    state = state,
    fileName = fileName,
    totalBytes = total,
    transferredBytes = transferred,
    speed = 0L,
    meanSpeed = 0L,
    priority = priority,
    notificationNumber = n,
    lastError = null,
    isForeignOverQuota = false,
    path = path,
)

/** Builds a seeded, random sequence of store inputs that follows the SDK's guarantees. */
private class SyncInputGenerator(seed: Int) {
    private val random = Random(seed)
    private val live = LinkedHashMap<Int, SyncTransferEntry>()
    private var notificationNumber = 0L
    private var nextTag = 1

    fun seedInput(): SyncTransferStoreInput {
        repeat(SEED_TAGS) {
            val direction = randomDirection()
            val states = if (direction == SyncTransferDirection.Upload) UPLOAD_STATES else DOWNLOAD_STATES
            val tag = nextTag++
            live[tag] = entry(
                tag,
                states.random(random),
                n = ++notificationNumber,
                direction = direction,
                total = TOTAL_BYTES,
                fileName = fileName(tag),
            )
        }
        return SyncTransferStoreInput.Feed(SyncTransferUpdate.Seed(live.values.toList()))
    }

    fun next(state: SyncTransferStoreState): SyncTransferStoreInput {
        val roll = random.nextInt(100)
        return when {
            roll < 70 -> batch()
            roll < 78 -> SyncTransferStoreInput.Feed(SyncTransferUpdate.Reseed(reconcilableEntries(), notificationNumber))
            roll < 80 -> SyncTransferStoreInput.Feed(SyncTransferUpdate.Seed(reconcilableEntries()))
            roll < 86 -> clearCompleted(state)
            roll < 89 -> SyncTransferStoreInput.ClearAllCompleted
            roll < 97 -> setPause(state)
            else -> SyncTransferStoreInput.RevertOptimisticPause(1 + random.nextInt(nextTag))
        }
    }

    private fun batch(): SyncTransferStoreInput {
        if (random.nextInt(100) < 2) return suspendBurst()
        val touched = HashSet<Int>()
        val events = ArrayList<SyncTransferEvent>()
        repeat(1 + random.nextInt(MAX_OPERATIONS)) {
            val event = if (live.size < MAX_LIVE && random.nextInt(100) < 40) {
                startEvent(touched)
            } else {
                changeOrFinish(touched)
            }
            if (event != null) events += event
        }
        return SyncTransferStoreInput.Feed(SyncTransferUpdate.Batch(events.filter { random.nextInt(100) >= 5 }))
    }

    private fun suspendBurst(): SyncTransferStoreInput {
        val events = live.keys.toList().map { tag ->
            finish(tag, startedAt = null, state = TransferState.STATE_CANCELLED)
        }
        return SyncTransferStoreInput.Feed(SyncTransferUpdate.Batch(events))
    }

    private fun startEvent(touched: MutableSet<Int>): SyncTransferEvent {
        val tag = nextTag++
        touched += tag
        val roll = random.nextInt(10)
        val state = when {
            roll < 8 -> TransferState.STATE_QUEUED
            roll == 8 -> TransferState.STATE_ACTIVE
            else -> TransferState.STATE_PAUSED
        }
        val startedAt = ++notificationNumber
        val started = entry(
            tag,
            state,
            n = startedAt,
            direction = randomDirection(),
            total = TOTAL_BYTES,
            fileName = fileName(tag),
        )
        live[tag] = started
        if (random.nextInt(100) >= 25) return SyncTransferEvent(SyncTransferEventType.Start, started, null)
        return change(tag, startedAt = startedAt)
    }

    private fun changeOrFinish(touched: MutableSet<Int>): SyncTransferEvent? {
        val candidates = live.keys.filter { it !in touched }
        if (candidates.isEmpty()) return null
        val tag = candidates.random(random)
        touched += tag
        return change(tag, startedAt = null)
    }

    private fun change(tag: Int, startedAt: Long?): SyncTransferEvent {
        val current = live.getValue(tag)
        if (current.state == TransferState.STATE_COMPLETING || random.nextInt(100) < 35) {
            return finish(tag, startedAt)
        }
        val states = if (current.direction == SyncTransferDirection.Upload) UPLOAD_STATES else DOWNLOAD_STATES
        val transferred = if (random.nextInt(10) == 0) {
            0L
        } else {
            minOf(TOTAL_BYTES, current.transferredBytes + random.nextInt(30))
        }
        val updated = entry(
            tag,
            states.random(random),
            n = ++notificationNumber,
            direction = current.direction,
            transferred = transferred,
            total = TOTAL_BYTES,
            fileName = fileName(tag),
        )
        live[tag] = updated
        return SyncTransferEvent(SyncTransferEventType.Update, updated, startedAt)
    }

    private fun finish(
        tag: Int,
        startedAt: Long?,
        state: TransferState = pickFinishState(),
    ): SyncTransferEvent {
        val current = live.remove(tag) ?: error("tag $tag is not live")
        val finished = entry(
            tag,
            state,
            n = ++notificationNumber,
            direction = current.direction,
            transferred = current.transferredBytes,
            total = TOTAL_BYTES,
            fileName = fileName(tag),
            path = "/sync/$tag",
        )
        return SyncTransferEvent(SyncTransferEventType.Finish, finished, startedAt)
    }

    private fun clearCompleted(state: SyncTransferStoreState): SyncTransferStoreInput {
        val tags = state.completed.map { it.entry.tag }
            .shuffled(random)
            .take(1 + random.nextInt(3))
            .toMutableSet()
        tags += 1 + random.nextInt(nextTag)
        return SyncTransferStoreInput.ClearCompleted(tags)
    }

    private fun setPause(state: SyncTransferStoreState): SyncTransferStoreInput {
        val candidates = (state.active.keys + state.overflow.keys).toList()
        val tags = HashSet<Int>()
        repeat(1 + random.nextInt(2)) {
            tags += if (candidates.isNotEmpty() && random.nextBoolean()) {
                candidates.random(random)
            } else {
                1 + random.nextInt(nextTag)
            }
        }
        return SyncTransferStoreInput.SetOptimisticPause(tags, random.nextBoolean())
    }

    private fun reconcilableEntries(): List<SyncTransferEntry> =
        live.values.filter { it.state != TransferState.STATE_COMPLETING }

    private fun pickFinishState(): TransferState {
        val roll = random.nextInt(100)
        return when {
            roll < 60 -> TransferState.STATE_COMPLETED
            roll < 80 -> TransferState.STATE_CANCELLED
            roll < 95 -> TransferState.STATE_FAILED
            else -> TransferState.STATE_ACTIVE
        }
    }

    private fun randomDirection(): SyncTransferDirection =
        if (random.nextBoolean()) SyncTransferDirection.Upload else SyncTransferDirection.Download

    private fun fileName(tag: Int): String = FILE_NAMES[(tag - 1) % FILE_NAMES.size]

    private companion object {
        const val SEED_TAGS = 12
        const val MAX_LIVE = 40
        const val MAX_OPERATIONS = 6
        const val TOTAL_BYTES = 100L
        val FILE_NAMES = listOf("a.jpg", "b.mp4", "c.pdf", "d.zip", "e.mp3", "f.bin")
        val UPLOAD_STATES = listOf(
            TransferState.STATE_QUEUED,
            TransferState.STATE_ACTIVE,
            TransferState.STATE_PAUSED,
            TransferState.STATE_RETRYING,
            TransferState.STATE_COMPLETING,
        )
        val DOWNLOAD_STATES = listOf(
            TransferState.STATE_QUEUED,
            TransferState.STATE_ACTIVE,
            TransferState.STATE_PAUSED,
            TransferState.STATE_RETRYING,
        )
    }
}
