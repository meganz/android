package mega.privacy.android.feature.sync.ui.mapper.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toImmutableList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveListItem
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveTransferUiModel
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressBody
import mega.privacy.android.feature.sync.ui.progress.active
import mega.privacy.android.feature.sync.ui.progress.blockers
import mega.privacy.android.feature.sync.ui.progress.entry
import mega.privacy.android.feature.sync.ui.progress.list
import mega.privacy.android.feature.sync.ui.progress.rowState
import mega.privacy.android.feature.sync.ui.progress.status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncProgressBodyMapperTest {

    private lateinit var underTest: SyncProgressBodyMapper

    private val activeMapper = mock<SyncActiveTransferUiModelMapper>()

    private val blockers = blockers()

    @BeforeEach
    fun setUp() {
        whenever(activeMapper.invoke(any(), any(), any(), any(), any())).thenAnswer {
            model(tag = (it.arguments[0] as SyncTransferListItem.Active).tag)
        }
        underTest = SyncProgressBodyMapper(activeMapper = activeMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(activeMapper)
    }

    @Test
    fun `test that invoke returns Loading when the status is not seeded`() {
        val rows = rows(1, 2)

        val result = map(status = status(isSeeded = false), list = list(rows))

        assertThat(result).isEqualTo(SyncProgressBody.Loading)
        verify(activeMapper, never()).invoke(any(), any(), any(), any(), any())
    }

    @Test
    fun `test that invoke returns Loading when the list has not arrived`() {
        assertThat(map(list = null)).isEqualTo(SyncProgressBody.Loading)
    }

    @Test
    fun `test that invoke returns Loading when the store is not seeded`() {
        assertThat(map(list = list(rows(1), isSeeded = false))).isEqualTo(SyncProgressBody.Loading)
    }

    @Test
    fun `test that invoke returns Loading when the root node is not ready`() {
        assertThat(map(list = list(rows(1)), isRootNodeReady = false))
            .isEqualTo(SyncProgressBody.Loading)
    }

    @Test
    fun `test that invoke returns Transfers with one row per active item in order`() {
        val rows = rows(5, 2, 9)

        val result = map(status = status(pending = 3), list = list(rows))

        assertThat(result).isEqualTo(
            SyncProgressBody.Transfers(
                items = listOf(5, 2, 9).map { SyncActiveListItem.Row(model(tag = it)) }
                    .toImmutableList(),
            )
        )
    }

    @Test
    fun `test that invoke passes each row, its row state, its band and the blockers to the row mapper`() {
        val rows = rows(1, 2, 3)
        val rowStates = rowStatesOf(rows)
        val bands = rows.associate { it.tag to it.tag + 10 }

        map(list = list(rows), rowStates = rowStates, bands = bands)

        rows.forEach { item ->
            verify(activeMapper).invoke(
                item,
                rowStates.getValue(item.tag),
                blockers,
                bands.getValue(item.tag),
                false,
            )
        }
    }

    @Test
    fun `test that invoke fails when a row has no bands entry`() {
        val rows = rows(4)
        val rowStates = rowStatesOf(rows)

        assertThrows<NoSuchElementException> {
            map(list = list(rows), rowStates = rowStates, bands = emptyMap())
        }
    }

    @Test
    fun `test that invoke appends the more waiting footer when pending items exceed the shown rows`() {
        val result = map(status = status(pending = 250), list = list(rows(1, 2, 3, 4, 5)))

        val items = (result as SyncProgressBody.Transfers).items
        assertThat(items).hasSize(6)
        assertThat(items.last()).isEqualTo(SyncActiveListItem.MoreWaiting(245))
    }

    @Test
    fun `test that invoke adds no footer when every pending item has a row`() {
        val result = map(status = status(pending = 5), list = list(rows(1, 2, 3, 4, 5)))

        val items = (result as SyncProgressBody.Transfers).items
        assertThat(items.filterIsInstance<SyncActiveListItem.MoreWaiting>()).isEmpty()
    }

    @Test
    fun `test that invoke never returns a negative more waiting count`() {
        val result = map(status = status(pending = 1), list = list(rows(1, 2, 3)))

        val items = (result as SyncProgressBody.Transfers).items
        assertThat(items).hasSize(3)
        assertThat(items.filterIsInstance<SyncActiveListItem.MoreWaiting>()).isEmpty()
    }

    @Test
    fun `test that invoke returns Transfers in every phase when rows exist`() {
        listOf(
            SyncProgressPhase.Paused(SyncPauseCause.User),
            SyncProgressPhase.Completed,
            SyncProgressPhase.UpToDate,
            SyncProgressPhase.Checking,
        ).forEach { phase ->
            val result = map(status = status(phase = phase), list = list(rows(1)))

            assertThat(result).isInstanceOf(SyncProgressBody.Transfers::class.java)
        }
    }

    @Test
    fun `test that invoke returns Loading when the list has no rows`() {
        assertThat(map(list = list())).isEqualTo(SyncProgressBody.Loading)
    }

    @Test
    fun `test that invoke passes the retrying transfer over quota signal to the row mapper`() {
        val rows = rows(1, 2)
        val rowStates = rowStatesOf(rows)
        val bands = rows.associate { it.tag to 1 }

        map(list = list(rows), rowStates = rowStates, bands = bands, isRetryingTransferOverQuota = true)

        rows.forEach { item ->
            verify(activeMapper).invoke(item, rowStates.getValue(item.tag), blockers, 1, true)
        }
    }

    private fun map(
        status: SyncProgressStatus = status(),
        list: SyncTransferList?,
        isRootNodeReady: Boolean = true,
        rowStates: Map<Int, SyncTransferRowState>? = null,
        bands: Map<Int, Int>? = null,
        isRetryingTransferOverQuota: Boolean = false,
    ): SyncProgressBody {
        val rows = list?.items?.filterIsInstance<SyncTransferListItem.Active>().orEmpty()
        return underTest(
            status = status,
            list = list,
            blockers = blockers,
            isRootNodeReady = isRootNodeReady,
            rowStates = rowStates ?: rowStatesOf(rows),
            bands = bands ?: rows.associate { it.tag to 1 },
            isRetryingTransferOverQuota = isRetryingTransferOverQuota,
        )
    }

    private fun rows(vararg tags: Int) = tags.map { active(entry(tag = it)) }

    private fun rowStatesOf(rows: List<SyncTransferListItem.Active>) =
        rows.associate { it.tag to rowState(progressPermille = it.tag) }

    private fun model(tag: Int) = SyncActiveTransferUiModel(
        tag = tag,
        fileName = "IMG_$tag.jpg",
        direction = SyncTransferDirection.Upload,
        status = SyncTransferStatus.Transferring,
        transferredBytes = 400L,
        totalBytes = 1_000L,
        progressPermille = 400,
        speed = 100L,
        etaSeconds = null,
        isOwnPause = false,
        isStorageOverQuota = false,
        isTransferOverQuota = false,
        hasIssues = false,
        lastErrorCode = null,
        isDraggable = true,
        band = 1,
    )
}
