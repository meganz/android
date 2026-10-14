package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferOverflowEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReconcileSyncTransfersUseCaseTest {

    private lateinit var underTest: ReconcileSyncTransfersUseCase

    private val monitorSyncRunSnapshotUseCase = mock<MonitorSyncRunSnapshotUseCase>()
    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()
    private val requestSyncTransferReseedUseCase = mock<RequestSyncTransferReseedUseCase>()

    private val runSnapshot = MutableStateFlow(SyncRunSnapshot.NotSeeded)
    private val store = MutableStateFlow(SyncTransferStoreState.NotSeeded)

    private val window = ReconcileSyncTransfersUseCase.RECONCILE_AFTER.inWholeMilliseconds
    private val oneMinute = 60_000L

    @BeforeAll
    fun setUp() {
        underTest = ReconcileSyncTransfersUseCase(
            monitorSyncRunSnapshotUseCase = monitorSyncRunSnapshotUseCase,
            monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
            requestSyncTransferReseedUseCase = requestSyncTransferReseedUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncRunSnapshotUseCase,
            monitorSyncTransferStoreUseCase,
            requestSyncTransferReseedUseCase,
        )
        runSnapshot.value = SyncRunSnapshot.NotSeeded
        store.value = SyncTransferStoreState.NotSeeded
        whenever(monitorSyncRunSnapshotUseCase()).thenReturn(runSnapshot)
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(store)
    }

    @Test
    fun `test that invoke requests a reseed when syncs report no pending work for the reconcile delay`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(window - 1)
            verifyNoInteractions(requestSyncTransferReseedUseCase)

            advance(1)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke requests at most one reseed per mismatch`() = runTest {
        runSnapshot.value = seeded(sync())
        store.value = storeWith(7)
        startReconciling()

        advance(window)
        advance(10 * oneMinute)

        verify(requestSyncTransferReseedUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke does nothing while a sync reports pending work`() = runTest {
        runSnapshot.value = seeded(
            sync(syncId = 1),
            sync(
                syncId = 2,
                stats = SyncStatsSample(false, false, pendingUploads = 2, pendingDownloads = 0),
            ),
        )
        store.value = storeWith(7)
        startReconciling()

        advance(oneMinute)

        verifyNoInteractions(requestSyncTransferReseedUseCase)
    }

    @Test
    fun `test that invoke does nothing while a sync reports scanning`() = runTest {
        runSnapshot.value = seeded(sync(stats = SyncStatsSample(isScanning = true, false, 0, 0)))
        store.value = storeWith(7)
        startReconciling()

        advance(oneMinute)

        verifyNoInteractions(requestSyncTransferReseedUseCase)
    }

    @ParameterizedTest
    @EnumSource(SyncRunState::class, names = ["Pending", "Loading"])
    fun `test that invoke does nothing while a sync is pending or loading`(runState: SyncRunState) =
        runTest {
            runSnapshot.value = seeded(
                sync(syncId = 1),
                sync(syncId = 2, runState = runState, stats = null),
            )
            store.value = storeWith(7)
            startReconciling()

            advance(oneMinute)

            verifyNoInteractions(requestSyncTransferReseedUseCase)
        }

    @Test
    fun `test that invoke does nothing when the store has no pending rows`() = runTest {
        runSnapshot.value = seeded(sync())
        store.value = SyncTransferStoreState.NotSeeded.copy(isSeeded = true)
        startReconciling()

        advance(oneMinute)

        verifyNoInteractions(requestSyncTransferReseedUseCase)
    }

    @Test
    fun `test that invoke does nothing before the run snapshot is seeded`() = runTest {
        runSnapshot.value = SyncRunSnapshot.NotSeeded
        store.value = storeWith(7)
        startReconciling()

        advance(oneMinute)

        verifyNoInteractions(requestSyncTransferReseedUseCase)
        assertThat(store.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `test that invoke does not subscribe to the store while a sync reports pending work`() =
        runTest {
            val busy = seeded(sync(stats = SyncStatsSample(false, false, 1, 0)))
            runSnapshot.value = busy
            store.value = storeWith(7)
            startReconciling()

            assertThat(store.subscriptionCount.value).isEqualTo(0)

            runSnapshot.value = seeded(sync())
            runCurrent()
            assertThat(store.subscriptionCount.value).isEqualTo(1)

            runSnapshot.value = busy
            runCurrent()
            assertThat(store.subscriptionCount.value).isEqualTo(0)
        }

    @Test
    fun `test that invoke counts overflow rows as pending rows`() = runTest {
        runSnapshot.value = seeded(sync())
        store.value = storeWith(overflow = setOf(9))
        startReconciling()

        advance(window)

        verify(requestSyncTransferReseedUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke requests a reseed when no sync exists`() = runTest {
        runSnapshot.value = seeded()
        store.value = storeWith(7)
        startReconciling()

        advance(window)

        verify(requestSyncTransferReseedUseCase, times(1)).invoke()
    }

    @Test
    fun `test that invoke requests a reseed for rows left after every sync was suspended`() =
        runTest {
            runSnapshot.value = seeded(
                sync(syncId = 1, runState = SyncRunState.Suspended, stats = null),
                sync(syncId = 2, runState = SyncRunState.Suspended, stats = null),
            )
            store.value = storeWith(7)
            startReconciling()

            advance(window)

            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke does not request a reseed while pause all empties the store`() =
        runTest {
            runSnapshot.value = seeded(
                sync(syncId = 1, runState = SyncRunState.Suspended, stats = null),
                sync(syncId = 2, runState = SyncRunState.Suspended, stats = null),
            )
            store.value = storeWith(7, 8)
            startReconciling()

            advance(1_000)
            store.value = storeWith(8)
            advance(1_000)
            store.value = SyncTransferStoreState.NotSeeded.copy(isSeeded = true)
            advance(oneMinute)

            verifyNoInteractions(requestSyncTransferReseedUseCase)
        }

    @Test
    fun `test that invoke treats a running sync without stats as reporting no pending work`() =
        runTest {
            runSnapshot.value = seeded(sync(stats = null))
            store.value = storeWith(7)
            startReconciling()

            advance(window)

            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke ignores the stats of a running sync with an error`() = runTest {
        runSnapshot.value = seeded(sync(hasError = true, stats = SyncStatsSample(false, false, 5, 0)))
        store.value = storeWith(7)
        startReconciling()

        advance(window)

        verify(requestSyncTransferReseedUseCase, times(1)).invoke()
    }

    @ParameterizedTest
    @EnumSource(SyncRunState::class, names = ["Suspended", "Disabled"])
    fun `test that invoke ignores the stats of a sync that is not running`(runState: SyncRunState) =
        runTest {
            runSnapshot.value = seeded(
                sync(runState = runState, stats = SyncStatsSample(false, false, 5, 0)),
            )
            store.value = storeWith(7)
            startReconciling()

            advance(window)

            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke restarts the reconcile delay when a sync reports pending work in between`() =
        runTest {
            val idle = seeded(sync())
            runSnapshot.value = idle
            store.value = storeWith(7)
            startReconciling()

            advance(window / 2)
            runSnapshot.value = seeded(sync(stats = SyncStatsSample(false, false, 1, 0)))
            advance(1_000)
            runSnapshot.value = idle
            advance(window - 1)
            verifyNoInteractions(requestSyncTransferReseedUseCase)

            advance(1)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke restarts the reconcile delay when the store changes during a mismatch`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(window - 4_000)
            store.value = storeWith(7, 8)
            advance(window - 1)
            verifyNoInteractions(requestSyncTransferReseedUseCase)

            advance(1)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke requests another reseed when the store changes after a request`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(window)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()

            store.value = storeWith(7).copy(reseedRemoved = persistentSetOf(9))
            advance(window)
            verify(requestSyncTransferReseedUseCase, times(2)).invoke()
        }

    @Test
    fun `test that invoke requests another reseed when the reseed keeps a completing row once`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(window)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()

            store.value = storeWith(7).copy(reseedKeptCompleting = persistentSetOf(7))
            advance(window)
            verify(requestSyncTransferReseedUseCase, times(2)).invoke()
        }

    @Test
    fun `test that invoke does not request again when the store republishes an equal state`() =
        runTest {
            val sharedStore = MutableSharedFlow<SyncTransferStoreState>(replay = 1)
            whenever(monitorSyncTransferStoreUseCase()).thenReturn(sharedStore)
            runSnapshot.value = seeded(sync())
            sharedStore.emit(storeWith(7))
            startReconciling()

            advance(window)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()

            val republished = storeWith(7)
            sharedStore.emit(republished)
            advance(window)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke ignores an equal store state republished during the reconcile delay`() =
        runTest {
            val sharedStore = MutableSharedFlow<SyncTransferStoreState>(replay = 1)
            whenever(monitorSyncTransferStoreUseCase()).thenReturn(sharedStore)
            runSnapshot.value = seeded(sync())
            sharedStore.emit(storeWith(7))
            startReconciling()

            advance(3_700)
            sharedStore.emit(storeWith(7))
            advance(window - 3_700 - 1)
            verifyNoInteractions(requestSyncTransferReseedUseCase)

            advance(1)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke does not request again when the store returns to the requested state`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(window)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()

            store.value = storeWith(7, 8)
            advance(window / 2)
            store.value = storeWith(7)
            advance(oneMinute)
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke keeps one store subscription when an idle run snapshot changes`() =
        runTest {
            runSnapshot.value = seeded(sync())
            store.value = storeWith(7)
            startReconciling()

            advance(3_000)
            runSnapshot.value = seeded(sync(stats = SyncStatsSample(false, isSyncing = true, 0, 0)))
            advance(window - 3_000)

            verify(monitorSyncTransferStoreUseCase, times(1)).invoke()
            verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke requests another reseed when a new mismatch starts`() = runTest {
        val idle = seeded(sync())
        runSnapshot.value = idle
        store.value = storeWith(7)
        startReconciling()

        advance(window)
        verify(requestSyncTransferReseedUseCase, times(1)).invoke()

        runSnapshot.value = seeded(sync(stats = SyncStatsSample(false, false, 1, 0)))
        runCurrent()
        runSnapshot.value = idle
        advance(window)
        verify(requestSyncTransferReseedUseCase, times(2)).invoke()
    }

    @Test
    fun `test that invoke keeps reconciling after a failed request`() = runTest {
        wheneverBlocking { requestSyncTransferReseedUseCase() }
            .thenThrow(RuntimeException("boom"))
            .thenReturn(Unit)
        runSnapshot.value = seeded(sync())
        store.value = storeWith(7)
        val reconciler = startReconciling()

        advance(window)
        verify(requestSyncTransferReseedUseCase, times(1)).invoke()
        assertThat(reconciler.isActive).isTrue()

        store.value = storeWith(7, 8)
        advance(window)
        verify(requestSyncTransferReseedUseCase, times(2)).invoke()
    }

    private fun TestScope.startReconciling() = backgroundScope.launch { underTest() }
        .also { runCurrent() }

    private fun TestScope.advance(millis: Long) {
        runCurrent()
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun sync(
        syncId: Long = 1L,
        runState: SyncRunState = SyncRunState.Running,
        stats: SyncStatsSample? = SyncStatsSample(false, false, 0, 0),
        hasError: Boolean = false,
    ) = SyncWorkState(
        syncId = syncId,
        runState = runState,
        isStorageOverQuota = false,
        stats = stats,
        runningSinceMillis = null,
        hasError = hasError,
    )

    private fun seeded(vararg syncs: SyncWorkState) = SyncRunSnapshot(
        isSeeded = true,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress.None,
    )

    private fun entry(tag: Int) = SyncTransferEntry(
        tag = tag,
        direction = SyncTransferDirection.Upload,
        state = TransferState.STATE_QUEUED,
        fileName = "file$tag.jpg",
        totalBytes = 100L,
        transferredBytes = 0L,
        speed = 0L,
        meanSpeed = 0L,
        priority = 0x800000000000L + tag,
        notificationNumber = tag.toLong(),
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )

    private fun storeWith(vararg tags: Int, overflow: Set<Int> = emptySet()) =
        SyncTransferStoreState.NotSeeded.copy(
            isSeeded = true,
            active = tags.associateWith {
                SyncTransferRow(entry(it), SyncMediaType.Image, optimisticPause = null)
            }.toPersistentHashMap(),
            overflow = overflow.associateWith {
                SyncTransferOverflowEntry(SyncTransferDirection.Upload, TransferState.STATE_QUEUED)
            }.toPersistentHashMap(),
        )
}
