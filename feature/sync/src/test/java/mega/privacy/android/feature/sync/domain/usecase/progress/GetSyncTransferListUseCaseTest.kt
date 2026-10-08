package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSyncTransferListUseCaseTest {

    private val getSyncTransferRowStateUseCase = GetSyncTransferRowStateUseCase()
    private val isSyncTransferSearchMatchUseCase = IsSyncTransferSearchMatchUseCase()
    private val getSyncTransferFilterCountsUseCase =
        GetSyncTransferFilterCountsUseCase(isSyncTransferSearchMatchUseCase)

    private lateinit var underTest: GetSyncTransferListUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetSyncTransferListUseCase(
            getSyncTransferRowStateUseCase = getSyncTransferRowStateUseCase,
            getSyncTransferFilterCountsUseCase = getSyncTransferFilterCountsUseCase,
            isSyncTransferSearchMatchUseCase = isSyncTransferSearchMatchUseCase,
        )
    }

    @Test
    fun `test that invoke orders completing before active before paused before finished`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_PAUSED, transferred = 10),
                row(2, state = TransferState.STATE_ACTIVE, transferred = 10),
                row(3, state = TransferState.STATE_COMPLETING),
            ),
            completed = listOf(finished(4, finishedAt = 1_000)),
        )

        val actual = invokeList(state, SyncTransferFilter.Media(SyncMediaType.Document))

        assertThat(actual.tags).containsExactly(3, 2, 1, 4).inOrder()
    }

    @Test
    fun `test that invoke orders the desktop example of completing queued transferring and paused rows`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_COMPLETING, priority = 9),
                row(
                    2,
                    direction = SyncTransferDirection.Download,
                    state = TransferState.STATE_ACTIVE,
                    priority = 5,
                    transferred = 10,
                ),
                row(3, state = TransferState.STATE_QUEUED, priority = 3),
                row(4, state = TransferState.STATE_PAUSED, priority = 1, transferred = 10),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Pending)

        assertThat(actual.tags).containsExactly(1, 3, 2, 4).inOrder()
        assertThat(actual.statuses).containsExactly(
            SyncTransferStatus.Completing,
            SyncTransferStatus.Queued,
            SyncTransferStatus.Transferring,
            SyncTransferStatus.Paused,
        ).inOrder()
    }

    @Test
    fun `test that invoke puts every pausable row in the paused band when all transfers are paused`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_QUEUED),
                row(2, state = TransferState.STATE_ACTIVE, transferred = 10),
                row(3, state = TransferState.STATE_COMPLETING),
            ),
        )

        val actual = invokeList(
            state,
            SyncTransferFilter.Pending,
            context = context(areTransfersPaused = true),
        )

        assertThat(actual.tags).containsExactly(3, 1, 2).inOrder()
        assertThat(actual.items.map { it.band })
            .containsExactly(0, 2, 2).inOrder()
        assertThat(actual.statuses).containsExactly(
            SyncTransferStatus.Completing,
            SyncTransferStatus.PausedInQueue,
            SyncTransferStatus.Paused,
        ).inOrder()
    }

    @Test
    fun `test that invoke moves an optimistically paused row to the paused band`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_ACTIVE, transferred = 10, optimisticPause = true),
                row(2, state = TransferState.STATE_QUEUED),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Pending)

        assertThat(actual.tags).containsExactly(2, 1).inOrder()
        assertThat(actual.items.last().band).isEqualTo(2)
    }

    @Test
    fun `test that invoke mixes directions by priority in the pending filter although the sdk orders only within a direction`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload, priority = 3),
                row(5, direction = SyncTransferDirection.Download, priority = 2),
                row(10, direction = SyncTransferDirection.Upload, priority = 1),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Pending)

        assertThat(actual.tags).containsExactly(10, 5, 1).inOrder()
    }

    @Test
    fun `test that invoke orders rows of one band by priority then tag`() {
        val state = storeState(
            active = listOf(row(7, priority = 100), row(4, priority = 100), row(9, priority = 50)),
        )

        val actual = invokeList(state, SyncTransferFilter.Pending)

        assertThat(actual.tags).containsExactly(9, 4, 7).inOrder()
    }

    @Test
    fun `test that invoke caps the rows at the visible limit and keeps the total`() {
        val state = storeState(active = (1..600).map { row(it) })

        val actual = invokeList(state, SyncTransferFilter.Pending, limit = 500)

        assertThat(actual.items).hasSize(500)
        assertThat(actual.totalCount).isEqualTo(600)
    }

    @Test
    fun `test that invoke keeps the lowest priority values when it caps the window`() {
        val priorities = (1L..600L).shuffled(Random(42))
        val state = storeState(
            active = priorities.mapIndexed { index, priority -> row(index + 1, priority = priority) },
        )

        val actual = invokeList(state, SyncTransferFilter.Pending, limit = 500)

        assertThat(actual.items.map { (it as SyncTransferListItem.Active).row.entry.priority })
            .containsExactlyElementsIn(1L..500L).inOrder()
    }

    @ParameterizedTest
    @ValueSource(ints = [20, 55])
    fun `test that invoke matches a full sort of the same rows`(limit: Int) {
        val random = Random(7)
        val states = listOf(
            TransferState.STATE_COMPLETING,
            TransferState.STATE_ACTIVE,
            TransferState.STATE_QUEUED,
            TransferState.STATE_PAUSED,
            TransferState.STATE_RETRYING,
        )
        val rows = (1..50).map {
            row(
                it,
                direction = if (it % 2 == 0) SyncTransferDirection.Upload else SyncTransferDirection.Download,
                state = states[it % states.size],
                priority = random.nextLong(1, 20),
                transferred = if (it % 3 == 0) 0 else 10,
            )
        }
        val completed = (0 until 10).map {
            finished(100 + it, finishedAt = 10_000L - (it / 2) * 1_000L, n = 20L - it)
        }
        val context = context()
        val activeItems = rows.map {
            val rowState = getSyncTransferRowStateUseCase(it, context, null)
            SyncTransferListItem.Active(it, rowState.status, rowState.isOwnPause)
        }
        val fullSort = activeItems.sortedWith(
            compareBy<SyncTransferListItem.Active>(
                { it.band },
                { it.row.entry.priority },
                { it.tag },
            )
        ) + completed.map { SyncTransferListItem.Finished(it) }

        val actual = invokeList(
            storeState(active = rows.shuffled(random), completed = completed),
            SyncTransferFilter.Media(SyncMediaType.Document),
            context = context,
            limit = limit,
        )

        assertThat(actual.items)
            .containsExactlyElementsIn(fullSort.take(limit))
            .inOrder()
    }

    @Test
    fun `test that invoke returns no rows when the limit is zero`() {
        val state = storeState(
            active = listOf(row(1), row(2)),
            completed = listOf(finished(3, finishedAt = 1_000)),
        )

        val actual = invokeList(state, SyncTransferFilter.Media(SyncMediaType.Document), limit = 0)

        assertThat(actual.items).isEmpty()
        assertThat(actual.totalCount).isEqualTo(3)
    }

    @Test
    fun `test that invoke returns every row when the limit is the maximum`() {
        val state = storeState(active = (1..30).map { row(it) })

        val actual = invokeList(state, SyncTransferFilter.Pending, limit = Int.MAX_VALUE)

        assertThat(actual.items).hasSize(30)
    }

    @Test
    fun `test that invoke filters pending rows by direction`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload),
                row(2, direction = SyncTransferDirection.Download),
                row(3, direction = SyncTransferDirection.Download),
                row(4, direction = SyncTransferDirection.Upload),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Direction(SyncTransferDirection.Download))

        assertThat(actual.tags).containsExactly(2, 3).inOrder()
        assertThat(actual.totalCount).isEqualTo(2)
    }

    @Test
    fun `test that invoke returns no completed rows for the pending filter`() {
        val state = storeState(
            active = listOf(row(1)),
            completed = listOf(finished(2, finishedAt = 1_000)),
        )

        val actual = invokeList(state, SyncTransferFilter.Pending)

        assertThat(actual.items.filterIsInstance<SyncTransferListItem.Finished>()).isEmpty()
        assertThat(actual.tags).containsExactly(1)
    }

    @Test
    fun `test that invoke returns no completed rows for a direction filter`() {
        val state = storeState(
            active = listOf(row(1, direction = SyncTransferDirection.Upload)),
            completed = listOf(
                finished(2, finishedAt = 1_000, direction = SyncTransferDirection.Upload),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Direction(SyncTransferDirection.Upload))

        assertThat(actual.items.filterIsInstance<SyncTransferListItem.Finished>()).isEmpty()
        assertThat(actual.totalCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke returns the completed rows in the store order for the completed filter`() {
        val state = storeState(
            active = listOf(row(9)),
            completed = listOf(
                finished(1, finishedAt = 2_000),
                finished(2, finishedAt = 1_000),
                finished(3, finishedAt = 3_000),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Completed)

        assertThat(actual.tags).containsExactly(1, 2, 3).inOrder()
        assertThat(actual.totalCount).isEqualTo(3)
    }

    @Test
    fun `test that invoke returns active and completed rows for a media type`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_ACTIVE, mediaType = SyncMediaType.Image),
                row(2, mediaType = SyncMediaType.Document),
            ),
            completed = listOf(
                finished(3, finishedAt = 2_000, mediaType = SyncMediaType.Image),
                finished(4, finishedAt = 1_000, mediaType = SyncMediaType.Document),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Media(SyncMediaType.Image))

        assertThat(actual.tags).containsExactly(1, 3).inOrder()
        assertThat(actual.totalCount).isEqualTo(2)
    }

    @ParameterizedTest
    @MethodSource("filtersThatCouldReachAnOverflowRow")
    fun `test that invoke never lists overflow rows`(filter: SyncTransferFilter) {
        val state = storeState(
            active = listOf(row(1, fileName = "f1.jpg", mediaType = SyncMediaType.Image)),
            overflow = mapOf(
                99 to overflow(SyncTransferDirection.Upload, TransferState.STATE_QUEUED, SyncMediaType.Image),
            ),
        )

        val actual = invokeList(state, filter)

        assertThat(actual.tags).containsExactly(1)
        assertThat(actual.totalCount).isEqualTo(1)
    }

    @Test
    fun `test that invoke matches the search text ignoring case`() {
        val state = storeState(
            active = listOf(row(1, fileName = "Report.PDF"), row(2, fileName = "notes.txt")),
        )

        val actual = invokeList(state, SyncTransferFilter.Search("rePort"))

        assertThat(actual.tags).containsExactly(1)
    }

    @Test
    fun `test that invoke matches the search text in the file name only`() {
        val state = storeState(
            completed = listOf(
                finished(1, finishedAt = 1_000, fileName = "notes.txt", path = "/Sync/report/notes.txt"),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Search("report"))

        assertThat(actual.items).isEmpty()
        assertThat(actual.totalCount).isEqualTo(0)
    }

    @Test
    fun `test that invoke matches the search text as given without trimming`() {
        val state = storeState(
            active = listOf(row(1, fileName = "Holiday.JPG"), row(2, fileName = "holiday-notes.txt")),
        )

        val actual = invokeList(state, SyncTransferFilter.Search("holiday "))

        assertThat(actual.items).isEmpty()
        assertThat(actual.totalCount).isEqualTo(0)
    }

    @Test
    fun `test that invoke narrows a search by direction`() {
        val state = storeState(
            active = listOf(
                row(1, direction = SyncTransferDirection.Upload, fileName = "a1.txt"),
                row(2, direction = SyncTransferDirection.Download, fileName = "a2.txt"),
                row(4, direction = SyncTransferDirection.Download, fileName = "b.txt"),
            ),
            completed = listOf(
                finished(3, finishedAt = 1_000, direction = SyncTransferDirection.Download, fileName = "a3.txt"),
                finished(5, finishedAt = 900, direction = SyncTransferDirection.Upload, fileName = "a5.txt"),
            ),
        )

        val actual = invokeList(state, SyncTransferFilter.Search("a", SyncTransferDirection.Download))

        assertThat(actual.tags).containsExactly(2, 3).inOrder()
        assertThat(actual.totalCount).isEqualTo(2)
    }

    @Test
    fun `test that invoke returns the filter counts of the store`() {
        val state = storeState(
            active = listOf(row(1, fileName = "a1.txt")),
            counts = SyncTransferCounts.Empty.copy(
                lifetimeUploads = SyncTransferTally(total = 2, pending = 1),
            ),
        )
        val filter = SyncTransferFilter.Search("a")

        val actual = invokeList(state, filter)

        assertThat(actual.counts).isEqualTo(getSyncTransferFilterCountsUseCase(state, filter))
        assertThat(actual.counts.searchUploads).isEqualTo(1)
    }

    @Test
    fun `test that invoke exposes the own pause flag of each row`() {
        val state = storeState(
            active = listOf(
                row(1, state = TransferState.STATE_PAUSED),
                row(2, state = TransferState.STATE_QUEUED),
            ),
        )

        val actual = invokeList(
            state,
            SyncTransferFilter.Pending,
            context = context(areTransfersPaused = true),
        ).items.filterIsInstance<SyncTransferListItem.Active>().associateBy { it.tag }

        assertThat(actual.getValue(1).isOwnPause).isTrue()
        assertThat(actual.getValue(2).isOwnPause).isFalse()
        assertThat(actual.getValue(2).status).isEqualTo(SyncTransferStatus.PausedInQueue)
    }

    @Test
    fun `test that invoke returns no rows before the store is seeded`() {
        val actual = invokeList(SyncTransferStoreState.NotSeeded, SyncTransferFilter.Pending)

        assertThat(actual.isSeeded).isFalse()
        assertThat(actual.items).isEmpty()
        assertThat(actual.totalCount).isEqualTo(0)
    }

    private fun filtersThatCouldReachAnOverflowRow(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncTransferFilter.Pending),
        Arguments.of(SyncTransferFilter.Direction(SyncTransferDirection.Upload)),
        Arguments.of(SyncTransferFilter.Media(SyncMediaType.Image)),
        Arguments.of(SyncTransferFilter.Search("f")),
    )

    private fun invokeList(
        state: SyncTransferStoreState,
        filter: SyncTransferFilter,
        context: SyncTransferRowContext = context(),
        limit: Int = GetSyncTransferListUseCase.VISIBLE_ROW_LIMIT,
    ) = underTest(state, filter, context, limit)

    private val SyncTransferList.tags get() = items.map { it.tag }

    private val SyncTransferList.statuses
        get() = items.map { (it as SyncTransferListItem.Active).status }

    private fun entry(
        tag: Int,
        direction: SyncTransferDirection,
        state: TransferState,
        priority: Long,
        transferred: Long,
        fileName: String,
        notificationNumber: Long = tag.toLong(),
        path: String? = null,
    ) = SyncTransferEntry(
        tag = tag,
        direction = direction,
        state = state,
        fileName = fileName,
        totalBytes = 100,
        transferredBytes = transferred,
        speed = 0,
        meanSpeed = 0,
        priority = priority,
        notificationNumber = notificationNumber,
        lastError = null,
        isForeignOverQuota = false,
        path = path,
    )

    private fun row(
        tag: Int,
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        state: TransferState = TransferState.STATE_QUEUED,
        priority: Long = tag.toLong(),
        transferred: Long = 0,
        fileName: String = "f$tag.txt",
        mediaType: SyncMediaType = SyncMediaType.Document,
        optimisticPause: Boolean? = null,
    ) = SyncTransferRow(
        entry = entry(tag, direction, state, priority, transferred, fileName),
        mediaType = mediaType,
        optimisticPause = optimisticPause,
    )

    private fun finished(
        tag: Int,
        finishedAt: Long,
        n: Long = tag.toLong(),
        direction: SyncTransferDirection = SyncTransferDirection.Upload,
        fileName: String = "f$tag.txt",
        mediaType: SyncMediaType = SyncMediaType.Document,
        path: String? = null,
    ) = SyncCompletedTransfer(
        entry = entry(
            tag = tag,
            direction = direction,
            state = TransferState.STATE_COMPLETED,
            priority = tag.toLong(),
            transferred = 100,
            fileName = fileName,
            notificationNumber = n,
            path = path,
        ),
        mediaType = mediaType,
        finishedAtMillis = finishedAt,
        batchId = 0,
    )

    private fun overflow(
        direction: SyncTransferDirection,
        state: TransferState,
        mediaType: SyncMediaType = SyncMediaType.Document,
    ) = SyncTransferOverflowEntry(direction, state, mediaType)

    private fun storeState(
        active: List<SyncTransferRow> = emptyList(),
        overflow: Map<Int, SyncTransferOverflowEntry> = emptyMap(),
        completed: List<SyncCompletedTransfer> = emptyList(),
        counts: SyncTransferCounts = SyncTransferCounts.Empty,
        isSeeded: Boolean = true,
    ) = SyncTransferStoreState(
        isSeeded = isSeeded,
        active = active.associateBy { it.entry.tag }.toPersistentHashMap(),
        overflow = overflow.toPersistentHashMap(),
        completed = completed.toPersistentList(),
        counts = counts,
        reseedRemoved = persistentHashSetOf(),
    )

    private fun context(
        areTransfersPaused: Boolean = false,
        storage: Boolean = false,
        transfer: Boolean = false,
    ) = SyncTransferRowContext(
        areTransfersPaused = areTransfersPaused,
        isStorageOverQuota = storage,
        isTransferOverQuota = transfer,
    )
}
