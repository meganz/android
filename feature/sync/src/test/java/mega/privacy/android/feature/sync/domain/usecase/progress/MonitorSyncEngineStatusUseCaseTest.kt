package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncCompletedTransfer
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEnginePauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineWork
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferTally
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncEngineStatusUseCaseTest {

    private val monitorSyncRunSnapshotUseCase = mock<MonitorSyncRunSnapshotUseCase>()
    private val monitorSyncProgressStatusUseCase = mock<MonitorSyncProgressStatusUseCase>()
    private val monitorSyncProgressBlockersUseCase = mock<MonitorSyncProgressBlockersUseCase>()
    private val monitorSyncEngineSignalsUseCase = mock<MonitorSyncEngineSignalsUseCase>()
    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()

    private val throttleMillis = MonitorSyncEngineStatusUseCase.THROTTLE.inWholeMilliseconds

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncRunSnapshotUseCase,
            monitorSyncProgressStatusUseCase,
            monitorSyncProgressBlockersUseCase,
            monitorSyncEngineSignalsUseCase,
            monitorSyncTransferStoreUseCase,
        )
    }

    @Test
    fun `test that invoke emits the calculated state of the inputs`() = runTest {
        val inputs = stubInputs(runSnapshot = runSnapshot(running(1, scanning = true)))
        val expected = GetSyncEngineStateUseCase().invoke(
            GetSyncEngineStateUseCase.Inputs(
                runSnapshot = inputs.runSnapshot.value,
                status = inputs.status.value,
                blockers = inputs.blockers.value,
                signals = SyncEngineSignals.NotWaiting,
                isStoreSeeded = true,
                pendingSyncTransfers = 0,
                nowMillis = 0,
            )
        )

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits the first status at once`() = runTest {
        stubInputs()

        createUnderTest()().test {
            runCurrent()
            awaitItem()
            assertThat(testScheduler.currentTime).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports NotSeeded until every input is seeded`() = runTest {
        val inputs = stubInputs(store = SyncTransferStoreState.NotSeeded)

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineState.NotSeeded)

            inputs.store.value = store()
            advanceTimeBy(throttleMillis)
            runCurrent()

            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke reports Paused with the user cause once pause all suspends every sync`() =
        runTest {
            val inputs = stubInputs(
                runSnapshot = runSnapshot(running(1, scanning = true), running(2, scanning = true))
            )

            createUnderTest()().test {
                runCurrent()
                assertThat(awaitItem().status)
                    .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
                advanceTimeBy(1_000)

                inputs.runSnapshot.value = runSnapshot(suspended(1), suspended(2))
                inputs.status.value = status(SyncProgressPhase.Paused(SyncPauseCause.User))
                advanceTimeBy(throttleMillis)
                runCurrent()

                val items = cancelAndConsumeRemainingEvents()
                    .filterIsInstance<Event.Item<SyncEngineState>>()
                    .map { it.value.status }
                assertThat(items.last()).isEqualTo(
                    SyncEngineStatus.Paused(SyncEnginePauseCause.Syncs(SyncPauseCause.User))
                )
            }
        }

    @Test
    fun `test that invoke recomputes when the pending sync transfer count changes`() = runTest {
        val inputs = stubInputs()

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))

            inputs.store.value = store(pending = 2)
            advanceTimeBy(throttleMillis)
            runCurrent()

            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Transferring))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke recomputes when the engine signals change`() = runTest {
        val inputs = stubInputs()

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))

            inputs.signals.emit(SyncEngineSignals(isWaiting = true, isSyncStalled = false))
            advanceTimeBy(throttleMillis)
            runCurrent()

            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Waiting))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke emits the latest status at most once per throttle window`() = runTest {
        val inputs = stubInputs()

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
            assertThat(testScheduler.currentTime).isEqualTo(0)

            advanceTimeBy(50)
            inputs.store.value = store(pending = 2)
            advanceTimeBy(50)
            inputs.signals.emit(SyncEngineSignals(isWaiting = true, isSyncStalled = false))
            advanceTimeBy(99)
            runCurrent()
            expectNoEvents()

            advanceTimeBy(1)
            runCurrent()

            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Waiting))
            assertThat(testScheduler.currentTime).isEqualTo(200)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke recomputes at reevaluateAtMillis`() = runTest {
        // Subscribing after the sync started keeps the cap off any fixed poll grid.
        advanceTimeBy(1_500)
        stubInputs(runSnapshot = runSnapshot(running(1, hasStats = false, since = 1_234)))

        createUnderTest()().test {
            runCurrent()
            val first = awaitItem()
            assertThat(first.status).isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
            assertThat(first.reevaluateAtMillis).isEqualTo(11_234)
            assertThat(testScheduler.currentTime).isEqualTo(1_500)

            advanceTimeBy(9_733)
            runCurrent()
            expectNoEvents()

            advanceTimeBy(1)
            runCurrent()

            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.UpToDate(isAccountFull = false))
            assertThat(testScheduler.currentTime).isEqualTo(11_234)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a new input cancels the scheduled recompute`() = runTest {
        val inputs = stubInputs(runSnapshot = runSnapshot(running(1, hasStats = false, since = 0)))

        createUnderTest()().test {
            runCurrent()
            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))

            advanceTimeBy(5_000)
            inputs.runSnapshot.value = runSnapshot(running(1, scanning = true))
            runCurrent()

            val current = awaitItem()
            assertThat(current.status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Scanning))
            assertThat(current.reevaluateAtMillis).isNull()
            assertThat(testScheduler.currentTime).isEqualTo(5_000)

            // A stale recompute at the old cap would report UpToDate here.
            advanceTimeBy(6_000)
            runCurrent()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that stats samples do not restart the waiting poll`() = runTest {
        val inputs = stubInputs()

        createUnderTest()().test {
            runCurrent()
            assertThat(inputs.signals.subscriptionCount.value).isEqualTo(1)

            (1..5).forEach { sample ->
                inputs.runSnapshot.value = runSnapshot(running(1, pendingUploads = sample))
                advanceTimeBy(throttleMillis)
                runCurrent()
                assertThat(inputs.signals.subscriptionCount.value).isEqualTo(1)
            }

            verify(monitorSyncEngineSignalsUseCase, times(1)).invoke()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke stops reading signals when no sync is running`() = runTest {
        val inputs = stubInputs(runSnapshot = runSnapshot(suspended(1)))

        createUnderTest()().test {
            runCurrent()
            verify(monitorSyncEngineSignalsUseCase, never()).invoke()

            inputs.runSnapshot.value = runSnapshot(running(1))
            advanceTimeBy(throttleMillis)
            runCurrent()
            verify(monitorSyncEngineSignalsUseCase, times(1)).invoke()
            assertThat(inputs.signals.subscriptionCount.value).isEqualTo(1)

            inputs.runSnapshot.value = runSnapshot(suspended(1))
            advanceTimeBy(throttleMillis)
            runCurrent()
            assertThat(inputs.signals.subscriptionCount.value).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke ignores the waiting signal while no sync is running`() = runTest {
        stubInputs(
            runSnapshot = runSnapshot(suspended(1), suspended(2)),
            signals = SyncEngineSignals(isWaiting = true, isSyncStalled = true),
        )

        createUnderTest()().test {
            runCurrent()
            val state = awaitItem()

            assertThat(state.status).isNotEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Waiting))
            assertThat(state.busyWork).isNotEqualTo(SyncEngineWork.Waiting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invoke does not emit an unchanged state`() = runTest {
        val inputs = stubInputs(store = store(pending = 1))
        val getSyncEngineStateUseCase = spy(GetSyncEngineStateUseCase())

        createUnderTest(getSyncEngineStateUseCase)().test {
            runCurrent()
            assertThat(awaitItem().status)
                .isEqualTo(SyncEngineStatus.Busy(SyncEngineWork.Transferring))
            advanceTimeBy(1_000)
            clearInvocations(getSyncEngineStateUseCase)

            val current = inputs.store.value
            inputs.store.value = current.copy(
                active = persistentMapOf(
                    1 to SyncTransferRow(entry(transferredBytes = 50), SyncMediaType.Image, null)
                ),
                completed = persistentListOf(
                    SyncCompletedTransfer(entry(transferredBytes = 100), SyncMediaType.Image, 900, 1)
                ),
                counts = current.counts.copy(
                    lifetimeUploads = SyncTransferTally(total = 2, pending = 1),
                ),
            )
            advanceTimeBy(1_000)
            runCurrent()

            verify(getSyncEngineStateUseCase, never()).invoke(any())
            expectNoEvents()

            inputs.runSnapshot.value = runSnapshot(running(1, pendingUploads = 5))
            advanceTimeBy(1_000)
            runCurrent()

            verify(getSyncEngineStateUseCase, times(1)).invoke(any())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.createUnderTest(
        getSyncEngineStateUseCase: GetSyncEngineStateUseCase = GetSyncEngineStateUseCase(),
    ) = MonitorSyncEngineStatusUseCase(
        monitorSyncRunSnapshotUseCase = monitorSyncRunSnapshotUseCase,
        monitorSyncProgressStatusUseCase = monitorSyncProgressStatusUseCase,
        monitorSyncProgressBlockersUseCase = monitorSyncProgressBlockersUseCase,
        monitorSyncEngineSignalsUseCase = monitorSyncEngineSignalsUseCase,
        monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
        getSyncEngineStateUseCase = getSyncEngineStateUseCase,
        currentTimeProvider = { testScheduler.currentTime },
        defaultDispatcher = StandardTestDispatcher(testScheduler),
    )

    private fun stubInputs(
        runSnapshot: SyncRunSnapshot = runSnapshot(running(1)),
        status: SyncProgressStatus = status(),
        blockers: SyncProgressBlockers = blockers(),
        signals: SyncEngineSignals = SyncEngineSignals.NotWaiting,
        store: SyncTransferStoreState = store(),
    ): Inputs {
        val inputs = Inputs(
            runSnapshot = MutableStateFlow(runSnapshot),
            status = MutableStateFlow(status),
            blockers = MutableStateFlow(blockers),
            signals = MutableSharedFlow<SyncEngineSignals>(replay = 1).apply { tryEmit(signals) },
            store = MutableStateFlow(store),
        )
        whenever(monitorSyncRunSnapshotUseCase()).thenReturn(inputs.runSnapshot)
        whenever(monitorSyncProgressStatusUseCase()).thenReturn(inputs.status)
        whenever(monitorSyncProgressBlockersUseCase()).thenReturn(inputs.blockers)
        whenever(monitorSyncEngineSignalsUseCase()).thenReturn(inputs.signals)
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(inputs.store)
        return inputs
    }

    private class Inputs(
        val runSnapshot: MutableStateFlow<SyncRunSnapshot>,
        val status: MutableStateFlow<SyncProgressStatus>,
        val blockers: MutableStateFlow<SyncProgressBlockers>,
        val signals: MutableSharedFlow<SyncEngineSignals>,
        val store: MutableStateFlow<SyncTransferStoreState>,
    )

    private fun runSnapshot(vararg syncs: SyncWorkState) = SyncRunSnapshot(
        isSeeded = true,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress.None,
    )

    private fun status(phase: SyncProgressPhase = SyncProgressPhase.UpToDate) =
        SyncProgressStatus(isSeeded = true, phase = phase, progress = SyncEpisodeProgress.None)

    private fun blockers() = SyncProgressBlockers(
        isOffline = false,
        isStorageOverQuota = false,
        isTransferOverQuota = false,
        areTransfersPaused = false,
        stalledIssueCount = 0,
    )

    private fun store(pending: Int = 0) = SyncTransferStoreState.NotSeeded.copy(
        isSeeded = true,
        counts = SyncTransferCounts.Empty.copy(
            lifetimeUploads = SyncTransferTally(total = pending, pending = pending),
        ),
    )

    private fun running(
        id: Long,
        scanning: Boolean = false,
        pendingUploads: Int = 0,
        since: Long = 0L,
        hasStats: Boolean = true,
    ) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = if (hasStats) SyncStatsSample(scanning, false, pendingUploads, 0) else null,
        runningSinceMillis = since,
    )

    private fun suspended(id: Long) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Suspended,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = null,
    )

    private fun entry(transferredBytes: Long) = SyncTransferEntry(
        tag = 1,
        direction = SyncTransferDirection.Upload,
        state = TransferState.STATE_ACTIVE,
        fileName = "a.jpg",
        totalBytes = 100,
        transferredBytes = transferredBytes,
        speed = 0,
        meanSpeed = 0,
        priority = 1,
        notificationNumber = 1,
        lastError = null,
        isForeignOverQuota = false,
        path = null,
    )
}
