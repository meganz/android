package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncTransferSpeedsUseCaseTest {

    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()
    private val getCurrentUploadSpeedUseCase = mock<GetCurrentUploadSpeedUseCase>()
    private val getCurrentDownloadSpeedUseCase = mock<GetCurrentDownloadSpeedUseCase>()

    private val noSpeeds = SyncTransferSpeeds(uploadSpeed = null, downloadSpeed = null)

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncTransferStoreUseCase,
            getCurrentUploadSpeedUseCase,
            getCurrentDownloadSpeedUseCase,
        )
    }

    @Test
    fun `test that invoke returns the smaller of the row sum and the sdk speed`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Upload, 300),
                    activeRow(2, SyncTransferDirection.Upload, 200),
                ),
                pendingUploads = 2,
            )
        )
        stubUploadSpeeds(400L, 900L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(400L)
            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(500L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits the first speeds at once`() = runTest {
        val underTest = createUseCase(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 100)), pendingUploads = 1)
        )
        stubUploadSpeeds(1_000L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncTransferSpeeds(uploadSpeed = 100L, downloadSpeed = null))
            assertThat(currentTime).isEqualTo(0L)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(1)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(1)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke hides the upload speed when no upload is pending`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Upload, 300),
                    activeRow(2, SyncTransferDirection.Download, 100),
                ),
                pendingDownloads = 1,
                completedUploads = 2,
            )
        )
        stubUploadSpeeds(1_000L)
        stubDownloadSpeeds(200)

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncTransferSpeeds(uploadSpeed = null, downloadSpeed = 100L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke hides the download speed when no download is pending`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Upload, 300),
                    activeRow(2, SyncTransferDirection.Download, 100),
                ),
                pendingUploads = 1,
                completedDownloads = 2,
            )
        )
        stubUploadSpeeds(1_000L)
        stubDownloadSpeeds(200)

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncTransferSpeeds(uploadSpeed = 300L, downloadSpeed = null))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits no speeds while nothing is pending`() = runTest {
        val underTest = createUseCase(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 300)))
        )

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(noSpeeds)
            advanceTimeBy(5_000L)
            runCurrent()
            expectNoEvents()
            verifyNoInteractions(getCurrentUploadSpeedUseCase, getCurrentDownloadSpeedUseCase)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits no speeds before the store is seeded`() = runTest {
        val underTest = createUseCase(SyncTransferStoreState.NotSeeded)

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(noSpeeds)
            advanceTimeBy(5_000L)
            runCurrent()
            expectNoEvents()
            verifyNoInteractions(getCurrentUploadSpeedUseCase, getCurrentDownloadSpeedUseCase)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke refreshes every speed refresh period`() = runTest {
        val underTest = createUseCase(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 1_000)), pendingUploads = 1)
        )
        stubUploadSpeeds(100L, 200L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(100L)
            advanceTimeBy(SPEED_REFRESH_MILLIS - 1)
            runCurrent()
            expectNoEvents()
            verifyBlocking(getCurrentUploadSpeedUseCase, times(1)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(1)) { invoke() }
            advanceTimeBy(1)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(200L)
            assertThat(currentTime).isEqualTo(SPEED_REFRESH_MILLIS)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(2)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that store publishes do not restart the speed poll`() = runTest {
        val storeFlow = MutableStateFlow(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 500)), pendingUploads = 1)
        )
        val underTest = createUseCase(storeFlow)
        stubUploadSpeeds(650L, 1_000L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(500L)

            advanceTimeBy(300L)
            storeFlow.value =
                store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 600)), pendingUploads = 1)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(600L)

            advanceTimeBy(300L)
            storeFlow.value =
                store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 700)), pendingUploads = 1)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(650L)

            advanceTimeBy(99L)
            runCurrent()
            verifyBlocking(getCurrentUploadSpeedUseCase, times(1)) { invoke() }

            advanceTimeBy(1L)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(700L)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke stops polling when no direction is pending`() = runTest {
        val storeFlow = MutableStateFlow(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 500)), pendingUploads = 1)
        )
        val underTest = createUseCase(storeFlow)
        stubUploadSpeeds(400L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(400L)
            advanceTimeBy(1_000L)
            storeFlow.value = store(rows = emptyList())
            runCurrent()
            assertThat(awaitItem()).isEqualTo(noSpeeds)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(2)) { invoke() }

            advanceTimeBy(4_000L)
            runCurrent()
            expectNoEvents()
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(2)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke polls again when a direction becomes pending again`() = runTest {
        val storeFlow = MutableStateFlow(store(rows = emptyList()))
        val underTest = createUseCase(storeFlow)
        stubUploadSpeeds(400L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(noSpeeds)
            advanceTimeBy(2_000L)
            storeFlow.value =
                store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 500)), pendingUploads = 1)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(400L)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(1)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(1)) { invoke() }

            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            assertThat(currentTime).isEqualTo(2_000L + SPEED_REFRESH_MILLIS)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            verifyBlocking(getCurrentDownloadSpeedUseCase, times(2)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke counts only rows whose sdk state is active`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Upload, 900, TransferState.STATE_QUEUED),
                    activeRow(2, SyncTransferDirection.Upload, 100),
                ),
                pendingUploads = 2,
            )
        )
        stubUploadSpeeds(1_000L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(100L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke follows the sdk speed down when row speeds are stale`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Upload, 300),
                    activeRow(2, SyncTransferDirection.Upload, 200),
                ),
                pendingUploads = 2,
            )
        )
        stubUploadSpeeds(500L, 250L, 0L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(500L)
            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(250L)
            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(0L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke falls back to the row sum when the sdk read fails`() = runTest {
        val underTest = createUseCase(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 500)), pendingUploads = 1)
        )
        getCurrentUploadSpeedUseCase.stub {
            onBlocking { invoke() }
                .thenThrow(RuntimeException("Speed read failed"))
                .thenReturn(400L)
        }
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(500L)
            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(400L)
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke converts the download speed to long before the minimum`() = runTest {
        val underTest = createUseCase(
            store(
                rows = listOf(
                    activeRow(1, SyncTransferDirection.Download, 1_500_000_000),
                    activeRow(2, SyncTransferDirection.Download, 1_500_000_000),
                ),
                pendingDownloads = 2,
            )
        )
        stubUploadSpeeds(0L)
        stubDownloadSpeeds(Int.MAX_VALUE)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().downloadSpeed).isEqualTo(2_147_483_647L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke does not emit unchanged speeds`() = runTest {
        val underTest = createUseCase(
            store(rows = listOf(activeRow(1, SyncTransferDirection.Upload, 500)), pendingUploads = 1)
        )
        stubUploadSpeeds(400L)
        stubDownloadSpeeds(0)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().uploadSpeed).isEqualTo(400L)
            advanceTimeBy(SPEED_REFRESH_MILLIS)
            runCurrent()
            verifyBlocking(getCurrentUploadSpeedUseCase, times(2)) { invoke() }
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.createUseCase(state: SyncTransferStoreState) =
        createUseCase(MutableStateFlow(state))

    private fun TestScope.createUseCase(
        storeFlow: MutableStateFlow<SyncTransferStoreState>,
    ): MonitorSyncTransferSpeedsUseCase {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(storeFlow)
        return MonitorSyncTransferSpeedsUseCase(
            monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
            getCurrentUploadSpeedUseCase = getCurrentUploadSpeedUseCase,
            getCurrentDownloadSpeedUseCase = getCurrentDownloadSpeedUseCase,
            getSyncTransferSpeedsUseCase = GetSyncTransferSpeedsUseCase(),
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
    }

    private fun stubUploadSpeeds(first: Long, vararg next: Long) {
        getCurrentUploadSpeedUseCase.stub {
            onBlocking { invoke() }.doReturn(first, *next.toTypedArray())
        }
    }

    private fun stubDownloadSpeeds(speed: Int) {
        getCurrentDownloadSpeedUseCase.stub {
            onBlocking { invoke() } doReturn speed
        }
    }

    private fun activeRow(
        tag: Int,
        direction: SyncTransferDirection,
        speed: Long,
        state: TransferState = TransferState.STATE_ACTIVE,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = direction,
            state = state,
            fileName = "file$tag.jpg",
            totalBytes = 100,
            transferredBytes = 10,
            speed = speed,
            meanSpeed = 0,
            priority = tag.toLong(),
            notificationNumber = tag.toLong(),
            lastError = null,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Image,
        optimisticPause = null,
    )

    private fun store(
        rows: List<SyncTransferRow>,
        pendingUploads: Int = 0,
        pendingDownloads: Int = 0,
        completedUploads: Int = 0,
        completedDownloads: Int = 0,
    ) = SyncTransferStoreState(
        isSeeded = true,
        active = rows.associateBy { it.entry.tag }.toPersistentMap(),
        overflow = persistentMapOf(),
        completed = persistentListOf(),
        counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(
                total = pendingUploads + completedUploads,
                pending = pendingUploads,
            ),
            lifetimeDownloads = SyncTransferTally(
                total = pendingDownloads + completedDownloads,
                pending = pendingDownloads,
            ),
        ),
        reseedRemoved = persistentSetOf(),
    )

    private companion object {
        val SPEED_REFRESH_MILLIS = MonitorSyncTransferSpeedsUseCase.SPEED_REFRESH.inWholeMilliseconds
    }
}
