package mega.privacy.android.feature.sync.ui.mapper.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowState
import mega.privacy.android.feature.sync.ui.model.progress.SyncCompletedTransferUiModel
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressTab
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressUiState
import mega.privacy.android.feature.sync.ui.progress.active
import mega.privacy.android.feature.sync.ui.progress.blockers
import mega.privacy.android.feature.sync.ui.progress.entry
import mega.privacy.android.feature.sync.ui.progress.finished
import mega.privacy.android.feature.sync.ui.progress.list
import mega.privacy.android.feature.sync.ui.progress.rowState
import mega.privacy.android.feature.sync.ui.progress.status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncProgressUiStateMapperTest {

    private lateinit var underTest: SyncProgressUiStateMapper

    private val bodyMapper = mock<SyncProgressBodyMapper>()

    private val activeBody = SyncProgressBody.Transfers(persistentListOf())

    @BeforeEach
    fun setUp() {
        whenever(bodyMapper.invoke(any(), anyOrNull(), any(), any(), any(), any(), any()))
            .thenReturn(activeBody)
        underTest = SyncProgressUiStateMapper(
            bodyMapper = bodyMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(bodyMapper)
    }

    @Test
    fun `test that invoke takes the active body from the body mapper`() {
        val status = status(pending = 3)
        val pendingList = list(listOf(active(entry(tag = 1))))
        val blockers = blockers(isOffline = true)
        val rowStates = mapOf(1 to rowState())
        val bands = mapOf(1 to 1)

        val result = map(
            status = status,
            pendingList = pendingList,
            blockers = blockers,
            isRootNodeReady = false,
            rowStates = rowStates,
            bands = bands,
        )

        assertThat(result.activeBody).isEqualTo(activeBody)
        verify(bodyMapper).invoke(
            status = status,
            list = pendingList,
            blockers = blockers,
            isRootNodeReady = false,
            rowStates = rowStates,
            bands = bands,
            isRetryingTransferOverQuota = false,
        )
    }

    @Test
    fun `test that invoke passes the row states and the bands to the body mapper`() {
        val rowStates = mapOf(1 to rowState(), 2 to rowState(progressPermille = 2))
        val bands = mapOf(1 to 1, 2 to 2)

        map(rowStates = rowStates, bands = bands)

        verify(bodyMapper).invoke(
            any(),
            anyOrNull(),
            any(),
            any(),
            same(rowStates),
            same(bands),
            any(),
        )
    }

    @Test
    fun `test that invoke keeps the completed body it is given`() {
        val completedBody = SyncProgressCompletedBody.Transfers(
            items = persistentListOf(
                SyncCompletedTransferUiModel(
                    tag = 1,
                    fileName = "IMG_1.jpg",
                    direction = SyncTransferDirection.Upload,
                    location = "Camera",
                    sizeBytes = 1_000L,
                    finishedAtMillis = 1_727_700_000_123L,
                    averageSpeed = 1_000L,
                ),
            ),
        )

        val result = map(completedList = list(listOf(finished(entry(tag = 1)))), completedBody = completedBody)

        assertThat(result.completedBody).isSameInstanceAs(completedBody)
    }

    @Test
    fun `test that invoke takes files left from the status`() {
        val result = map(
            status = status(phase = SyncProgressPhase.Paused(SyncPauseCause.User), interrupted = 40),
        )

        assertThat(result.filesLeft).isEqualTo(40)
    }

    @Test
    fun `test that invoke takes the app wide pause from the blockers`() {
        assertThat(map(blockers = blockers(areTransfersPaused = true)).areTransfersPaused).isTrue()
    }

    @Test
    fun `test that invoke takes the filter counts from the pending list`() {
        val pendingList = list(counts = counts(pending = 7))

        val result = map(pendingList = pendingList, completedList = list(counts = counts(pending = 1)))

        assertThat(result.filterCounts).isEqualTo(pendingList.counts)
    }

    @Test
    fun `test that invoke takes the filter counts from the completed list before the pending list arrives`() {
        val completedList = list(counts = counts(pending = 3))

        val result = map(pendingList = null, completedList = completedList)

        assertThat(result.filterCounts).isEqualTo(completedList.counts)
    }

    @Test
    fun `test that invoke leaves the filter counts empty before any list arrives`() {
        val result = map(pendingList = null, completedList = null)

        assertThat(result.filterCounts).isEqualTo(SyncTransferFilterCounts.Empty)
    }

    @Test
    fun `test that invoke leaves the selected tab on active`() {
        assertThat(map().selectedTab).isEqualTo(SyncProgressTab.Active)
    }

    @Test
    fun `test that invoke passes the retrying transfer over quota signal to the body mapper`() {
        map(isRetryingTransferOverQuota = true)

        verify(bodyMapper).invoke(
            any(),
            anyOrNull(),
            any(),
            any(),
            any(),
            any(),
            eq(true),
        )
    }

    private fun map(
        status: SyncProgressStatus = status(),
        pendingList: SyncTransferList? = list(),
        completedList: SyncTransferList? = list(),
        completedBody: SyncProgressCompletedBody = SyncProgressCompletedBody.Loading,
        blockers: SyncProgressBlockers = blockers(),
        isRootNodeReady: Boolean = true,
        rowStates: Map<Int, SyncTransferRowState> = emptyMap(),
        bands: Map<Int, Int> = emptyMap(),
        isRetryingTransferOverQuota: Boolean = false,
    ): SyncProgressUiState = underTest(
        status = status,
        pendingList = pendingList,
        completedList = completedList,
        completedBody = completedBody,
        blockers = blockers,
        isRootNodeReady = isRootNodeReady,
        rowStates = rowStates,
        bands = bands,
        isRetryingTransferOverQuota = isRetryingTransferOverQuota,
    )

    private fun counts(pending: Int) = SyncTransferFilterCounts(
        pending = pending,
        pendingUploads = pending,
        pendingDownloads = 0,
        completed = 2,
        byMediaType = persistentMapOf(SyncMediaType.Image to 9),
        searchUploads = 0,
        searchDownloads = 0,
    )
}
