package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartSyncProgressMonitoringUseCaseImplTest {

    private lateinit var underTest: StartSyncProgressMonitoringUseCaseImpl

    private val monitorLoggedInRootNodeUseCase = mock<MonitorLoggedInRootNodeUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val runSyncRunSnapshotUseCase = mock<RunSyncRunSnapshotUseCase>()
    private val resetSyncRunSnapshotUseCase = mock<ResetSyncRunSnapshotUseCase>()
    private val runSyncTransferStoreUseCase = mock<RunSyncTransferStoreUseCase>()
    private val resetSyncTransferStoreUseCase = mock<ResetSyncTransferStoreUseCase>()

    private val rootA = NodeId(1L)
    private val rootB = NodeId(2L)

    private lateinit var session: MutableStateFlow<NodeId?>
    private lateinit var runSnapshotRun: SuspendedRun
    private lateinit var storeRun: SuspendedRun

    @BeforeAll
    fun setUp() {
        underTest = StartSyncProgressMonitoringUseCaseImpl(
            monitorLoggedInRootNodeUseCase = monitorLoggedInRootNodeUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            runSyncRunSnapshotUseCase = runSyncRunSnapshotUseCase,
            resetSyncRunSnapshotUseCase = resetSyncRunSnapshotUseCase,
            runSyncTransferStoreUseCase = runSyncTransferStoreUseCase,
            resetSyncTransferStoreUseCase = resetSyncTransferStoreUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorLoggedInRootNodeUseCase,
            getFeatureFlagValueUseCase,
            runSyncRunSnapshotUseCase,
            resetSyncRunSnapshotUseCase,
            runSyncTransferStoreUseCase,
            resetSyncTransferStoreUseCase,
        )
        session = MutableStateFlow(null)
        runSnapshotRun = SuspendedRun()
        storeRun = SuspendedRun()
        whenever(monitorLoggedInRootNodeUseCase()).thenReturn(session)
        wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.SyncProgress) }.thenReturn(true)
        wheneverBlocking { runSyncRunSnapshotUseCase() }.doSuspendableAnswer { runSnapshotRun.await() }
        wheneverBlocking { runSyncTransferStoreUseCase() }.doSuspendableAnswer { storeRun.await() }
    }

    @Test
    fun `test that invoke starts nothing while no account is logged in`() = runTest {
        backgroundScope.launch { underTest() }
        runCurrent()

        verifyNoInteractions(
            getFeatureFlagValueUseCase,
            runSyncRunSnapshotUseCase,
            resetSyncRunSnapshotUseCase,
            runSyncTransferStoreUseCase,
            resetSyncTransferStoreUseCase,
        )
    }

    @Test
    fun `test that invoke resets and then runs the run snapshot and the store when an account logs in with the flag enabled`() =
        runTest {
            val runStartsAtReset = mutableListOf<Int>()
            wheneverBlocking { resetSyncRunSnapshotUseCase() }.doSuspendableAnswer {
                runStartsAtReset += runSnapshotRun.starts + storeRun.starts
            }
            wheneverBlocking { resetSyncTransferStoreUseCase() }.doSuspendableAnswer {
                runStartsAtReset += runSnapshotRun.starts + storeRun.starts
            }
            backgroundScope.launch { underTest() }
            runCurrent()

            session.value = rootA
            runCurrent()

            val order = inOrder(
                getFeatureFlagValueUseCase,
                resetSyncRunSnapshotUseCase,
                resetSyncTransferStoreUseCase,
                runSyncRunSnapshotUseCase,
                runSyncTransferStoreUseCase,
            )
            order.verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.SyncProgress)
            order.verify(resetSyncRunSnapshotUseCase).invoke()
            order.verify(resetSyncTransferStoreUseCase).invoke()
            order.verify(runSyncRunSnapshotUseCase).invoke()
            order.verify(runSyncTransferStoreUseCase).invoke()
            assertThat(runStartsAtReset).containsExactly(0, 0)
            assertThat(runSnapshotRun.isRunning).isTrue()
            assertThat(storeRun.isRunning).isTrue()
        }

    @Test
    fun `test that invoke touches neither the run snapshot nor the store when the flag is disabled`() =
        runTest {
            wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.SyncProgress) }.thenReturn(false)
            backgroundScope.launch { underTest() }
            runCurrent()

            session.value = rootA
            runCurrent()

            verifyNoInteractions(
                runSyncRunSnapshotUseCase,
                resetSyncRunSnapshotUseCase,
                runSyncTransferStoreUseCase,
                resetSyncTransferStoreUseCase,
            )
            assertThat(runSnapshotRun.starts).isEqualTo(0)
            assertThat(storeRun.starts).isEqualTo(0)
        }

    @Test
    fun `test that invoke treats a failing flag read as disabled`() = runTest {
        wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.SyncProgress) }
            .thenThrow(RuntimeException("flag read failed"))
        backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        runCurrent()

        verifyNoInteractions(
            runSyncRunSnapshotUseCase,
            resetSyncRunSnapshotUseCase,
            runSyncTransferStoreUseCase,
            resetSyncTransferStoreUseCase,
        )
        assertThat(runSnapshotRun.starts).isEqualTo(0)
        assertThat(storeRun.starts).isEqualTo(0)

        session.value = null
        runCurrent()
        doReturn(true).whenever(getFeatureFlagValueUseCase).invoke(ApiFeatures.SyncProgress)
        session.value = rootB
        runCurrent()

        verify(resetSyncRunSnapshotUseCase).invoke()
        verify(resetSyncTransferStoreUseCase).invoke()
        assertThat(runSnapshotRun.isRunning).isTrue()
        assertThat(storeRun.isRunning).isTrue()
    }

    @Test
    fun `test that invoke reads the flag once per session`() = runTest {
        backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        advanceTimeBy(60_000)
        runCurrent()

        verify(getFeatureFlagValueUseCase, times(1)).invoke(ApiFeatures.SyncProgress)
    }

    @Test
    fun `test that a session cancelled during the flag read resets nothing`() = runTest {
        wheneverBlocking { getFeatureFlagValueUseCase(ApiFeatures.SyncProgress) }
            .doSuspendableAnswer { awaitCancellation() }
        backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        runCurrent()
        session.value = null
        runCurrent()

        verifyNoInteractions(
            resetSyncRunSnapshotUseCase,
            resetSyncTransferStoreUseCase,
            runSyncRunSnapshotUseCase,
            runSyncTransferStoreUseCase,
        )
    }

    @Test
    fun `test that invoke resets the run snapshot and the store when the account logs out`() =
        runTest {
            var completedRunSnapshotResets = 0
            var completedStoreResets = 0
            wheneverBlocking { resetSyncRunSnapshotUseCase() }.doSuspendableAnswer {
                yield()
                completedRunSnapshotResets += 1
            }
            wheneverBlocking { resetSyncTransferStoreUseCase() }.doSuspendableAnswer {
                yield()
                completedStoreResets += 1
            }
            backgroundScope.launch { underTest() }
            runCurrent()
            session.value = rootA
            runCurrent()

            session.value = null
            runCurrent()

            verify(resetSyncRunSnapshotUseCase, times(2)).invoke()
            verify(resetSyncTransferStoreUseCase, times(2)).invoke()
            assertThat(completedRunSnapshotResets).isEqualTo(2)
            assertThat(completedStoreResets).isEqualTo(2)
            assertThat(runSnapshotRun.cancellations).isEqualTo(1)
            assertThat(storeRun.cancellations).isEqualTo(1)
            assertThat(runSnapshotRun.isRunning).isFalse()
            assertThat(storeRun.isRunning).isFalse()
        }

    @Test
    fun `test that invoke resets and starts a new session when another account logs in`() =
        runTest {
            backgroundScope.launch { underTest() }
            runCurrent()
            session.value = rootA
            runCurrent()

            session.value = rootB
            runCurrent()

            val order = inOrder(
                getFeatureFlagValueUseCase,
                resetSyncRunSnapshotUseCase,
                resetSyncTransferStoreUseCase,
            )
            order.verify(resetSyncRunSnapshotUseCase).invoke()
            order.verify(resetSyncTransferStoreUseCase).invoke()
            order.verify(resetSyncRunSnapshotUseCase).invoke()
            order.verify(resetSyncTransferStoreUseCase).invoke()
            order.verify(getFeatureFlagValueUseCase).invoke(ApiFeatures.SyncProgress)
            order.verify(resetSyncRunSnapshotUseCase).invoke()
            order.verify(resetSyncTransferStoreUseCase).invoke()
            verify(getFeatureFlagValueUseCase, times(2)).invoke(ApiFeatures.SyncProgress)
            assertThat(runSnapshotRun.starts).isEqualTo(2)
            assertThat(runSnapshotRun.cancellations).isEqualTo(1)
            assertThat(storeRun.starts).isEqualTo(2)
            assertThat(storeRun.cancellations).isEqualTo(1)
            assertThat(runSnapshotRun.isRunning).isTrue()
            assertThat(storeRun.isRunning).isTrue()
        }

    @Test
    fun `test that invoke does not restart the session when the same account is emitted again`() =
        runTest {
            whenever(monitorLoggedInRootNodeUseCase()).thenReturn(
                flow {
                    emit(rootA)
                    emit(rootA)
                    awaitCancellation()
                }
            )
            backgroundScope.launch { underTest() }
            runCurrent()

            verify(getFeatureFlagValueUseCase, times(1)).invoke(ApiFeatures.SyncProgress)
            verify(resetSyncRunSnapshotUseCase, times(1)).invoke()
            verify(resetSyncTransferStoreUseCase, times(1)).invoke()
        }

    @Test
    fun `test that invoke does not restart the session when the monitor recovers with the same account`() =
        runTest {
            var collections = 0
            whenever(monitorLoggedInRootNodeUseCase()).thenReturn(
                flow {
                    collections++
                    emit(rootA)
                    if (collections == 1) throw RuntimeException("monitor failed")
                    awaitCancellation()
                }
            )
            backgroundScope.launch { underTest() }
            runCurrent()

            advanceTimeBy(1_001)
            runCurrent()

            assertThat(collections).isEqualTo(2)
            verify(getFeatureFlagValueUseCase, times(1)).invoke(ApiFeatures.SyncProgress)
            verify(resetSyncRunSnapshotUseCase, times(1)).invoke()
            verify(resetSyncTransferStoreUseCase, times(1)).invoke()
            assertThat(runSnapshotRun.starts).isEqualTo(1)
            assertThat(storeRun.starts).isEqualTo(1)
            assertThat(runSnapshotRun.isRunning).isTrue()
            assertThat(storeRun.isRunning).isTrue()
        }

    @Test
    fun `test that invoke retries the monitor after a failure`() = runTest {
        var collections = 0
        whenever(monitorLoggedInRootNodeUseCase()).thenReturn(
            flow<NodeId?> {
                collections++
                if (collections == 1) throw RuntimeException("monitor failed")
                awaitCancellation()
            }
        )
        backgroundScope.launch { underTest() }
        runCurrent()

        advanceTimeBy(999)
        runCurrent()
        assertThat(collections).isEqualTo(1)

        advanceTimeBy(1)
        runCurrent()
        assertThat(collections).isEqualTo(2)
    }

    @Test
    fun `test that invoke backs off the monitor retries from one second to one minute`() = runTest {
        val collectionTimes = mutableListOf<Long>()
        whenever(monitorLoggedInRootNodeUseCase()).thenReturn(
            flow<NodeId?> {
                collectionTimes += testScheduler.currentTime
                throw RuntimeException("monitor failed")
            }
        )
        backgroundScope.launch { underTest() }
        runCurrent()

        advanceTimeBy(183_000)
        runCurrent()

        assertThat(collectionTimes).containsExactly(
            0L, 1_000L, 3_000L, 7_000L, 15_000L, 31_000L, 63_000L, 123_000L, 183_000L,
        ).inOrder()
    }

    @Test
    fun `test that a failing run snapshot reset does not skip the store reset`() = runTest {
        wheneverBlocking { resetSyncRunSnapshotUseCase() }.thenThrow(RuntimeException("reset failed"))
        val runner = backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        runCurrent()

        verify(resetSyncTransferStoreUseCase, times(1)).invoke()
        assertThat(runSnapshotRun.isRunning).isTrue()
        assertThat(storeRun.isRunning).isTrue()
        assertThat(runner.isActive).isTrue()
    }

    @Test
    fun `test that a failing store run does not stop the run snapshot run`() = runTest {
        doThrow(RuntimeException("store failed")).whenever(runSyncTransferStoreUseCase).invoke()
        val runner = backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        runCurrent()

        assertThat(runSnapshotRun.isRunning).isTrue()
        verify(resetSyncRunSnapshotUseCase, times(1)).invoke()
        assertThat(runner.isActive).isTrue()
    }

    @Test
    fun `test that a failing run snapshot run does not stop the store run`() = runTest {
        doThrow(RuntimeException("run snapshot failed")).whenever(runSyncRunSnapshotUseCase).invoke()
        val runner = backgroundScope.launch { underTest() }
        runCurrent()

        session.value = rootA
        runCurrent()

        assertThat(storeRun.isRunning).isTrue()
        verify(resetSyncTransferStoreUseCase, times(1)).invoke()
        assertThat(runner.isActive).isTrue()
    }

    @Test
    fun `test that invoke keeps the session running while the account stays logged in`() =
        runTest {
            backgroundScope.launch { underTest() }
            runCurrent()
            session.value = rootA
            runCurrent()

            advanceTimeBy(3_600_000)
            runCurrent()

            assertThat(runSnapshotRun.isRunning).isTrue()
            assertThat(storeRun.isRunning).isTrue()
            assertThat(runSnapshotRun.starts).isEqualTo(1)
            assertThat(storeRun.starts).isEqualTo(1)
            verify(resetSyncRunSnapshotUseCase, times(1)).invoke()
            verify(resetSyncTransferStoreUseCase, times(1)).invoke()
        }

    @Test
    fun `test that the session-end resets run only after both runs have stopped`() = runTest {
        runSnapshotRun = SuspendedRun(cancellationDelayMillis = 1_000)
        storeRun = SuspendedRun(cancellationDelayMillis = 1_000)
        val runningAtRunSnapshotReset = mutableListOf<Boolean>()
        val runningAtStoreReset = mutableListOf<Boolean>()
        wheneverBlocking { resetSyncRunSnapshotUseCase() }.doSuspendableAnswer {
            val running = runSnapshotRun.isRunning || storeRun.isRunning
            yield()
            runningAtRunSnapshotReset += running
        }
        wheneverBlocking { resetSyncTransferStoreUseCase() }.doSuspendableAnswer {
            val running = runSnapshotRun.isRunning || storeRun.isRunning
            yield()
            runningAtStoreReset += running
        }
        backgroundScope.launch { underTest() }
        runCurrent()
        session.value = rootA
        runCurrent()

        session.value = null
        runCurrent()
        advanceTimeBy(999)
        runCurrent()

        verify(resetSyncRunSnapshotUseCase, times(1)).invoke()
        verify(resetSyncTransferStoreUseCase, times(1)).invoke()

        advanceTimeBy(1)
        runCurrent()

        verify(resetSyncRunSnapshotUseCase, times(2)).invoke()
        verify(resetSyncTransferStoreUseCase, times(2)).invoke()
        assertThat(runningAtRunSnapshotReset).containsExactly(false, false).inOrder()
        assertThat(runningAtStoreReset).containsExactly(false, false).inOrder()
    }

    private class SuspendedRun(private val cancellationDelayMillis: Long = 0) {
        var starts = 0
        var cancellations = 0
        val isRunning get() = starts > cancellations

        suspend fun await(): Nothing {
            starts++
            try {
                awaitCancellation()
            } finally {
                if (cancellationDelayMillis > 0) {
                    withContext(NonCancellable) { delay(cancellationDelayMillis) }
                }
                cancellations++
            }
        }
    }
}
