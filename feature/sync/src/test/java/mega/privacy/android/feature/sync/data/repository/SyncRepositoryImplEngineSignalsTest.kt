package mega.privacy.android.feature.sync.data.repository

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEngineSignals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncRepositoryImplEngineSignalsTest {

    private val megaApiGateway = mock<MegaApiGateway>()
    private val syncGateway = mock<SyncGateway>()

    private lateinit var globalUpdates: MutableSharedFlow<GlobalUpdate>
    private var waitingReason = 0
    private var isStalled = false
    private var failRead = false
    private val onIoDispatcher = ThreadLocal.withInitial { false }
    private val readsOnIo = mutableListOf<Boolean>()

    private val waiting = SyncEngineSignals(isWaiting = true, isSyncStalled = false)

    @BeforeEach
    fun resetMocks() {
        reset(megaApiGateway, syncGateway)
        globalUpdates = MutableSharedFlow()
        waitingReason = 0
        isStalled = false
        failRead = false
        readsOnIo.clear()
        whenever(megaApiGateway.globalUpdates).thenReturn(globalUpdates)
        whenever(megaApiGateway.getWaitingReason()).doAnswer {
            readsOnIo += onIoDispatcher.get()
            if (failRead) throw RuntimeException("read failed")
            waitingReason
        }
        whenever(syncGateway.isSyncStalled()).doAnswer {
            readsOnIo += onIoDispatcher.get()
            isStalled
        }
    }

    private fun TestScope.createUnderTest() = SyncRepositoryImpl(
        syncWorkManagerGateway = mock(),
        syncGateway = syncGateway,
        syncStatsCacheGateway = mock(),
        megaApiGateway = megaApiGateway,
        folderPairMapper = mock(),
        stalledIssuesMapper = mock(),
        syncErrorMapper = mock(),
        syncTypeMapper = mock(),
        syncByWifiToNetworkTypeMapper = mock(),
        ioDispatcher = MarkingDispatcher(StandardTestDispatcher(testScheduler), onIoDispatcher),
        appScope = backgroundScope,
    )

    @Test
    fun `test that monitorSyncEngineSignals emits the first signals at once`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
            assertThat(currentTime).isEqualTo(0)
            verify(megaApiGateway, times(1)).getWaitingReason()
            verify(syncGateway, times(1)).isSyncStalled()
            assertThat(readsOnIo).containsExactly(true, true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "code {0}")
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `test that monitorSyncEngineSignals reports waiting for any non zero code`(code: Int) =
        runTest {
            waitingReason = code

            createUnderTest().monitorSyncEngineSignals().test {
                runCurrent()
                assertThat(awaitItem().isWaiting).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitorSyncEngineSignals reports not waiting for code zero`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem().isWaiting).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals reports the stall flag`() = runTest {
        isStalled = true

        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem())
                .isEqualTo(SyncEngineSignals(isWaiting = false, isSyncStalled = true))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals reads again when the poll elapses`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            advanceTimeBy(4_999)
            runCurrent()
            waitingReason = 1
            expectNoEvents()
            assertThat(currentTime).isEqualTo(4_999)
            verify(megaApiGateway, times(1)).getWaitingReason()

            advanceTimeBy(1)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(waiting)
            assertThat(currentTime).isEqualTo(5_000)
            verify(megaApiGateway, times(2)).getWaitingReason()
            assertThat(readsOnIo).containsExactly(true, true, true, true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals re-reads on a global sync state change`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            waitingReason = 2
            advanceTimeBy(1_000)
            runCurrent()
            globalUpdates.emit(GlobalUpdate.OnGlobalSyncStateChanged)
            runCurrent()

            assertThat(awaitItem()).isEqualTo(waiting)
            assertThat(currentTime).isEqualTo(1_000)
            assertThat(readsOnIo).containsExactly(true, true, true, true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals ignores other global updates`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            waitingReason = 2
            advanceTimeBy(1_000)
            runCurrent()
            globalUpdates.emit(GlobalUpdate.OnReloadNeeded)
            globalUpdates.emit(GlobalUpdate.OnAccountUpdate)
            runCurrent()

            expectNoEvents()
            verify(megaApiGateway, times(1)).getWaitingReason()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals emits distinct values only`() = runTest {
        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            advanceTimeBy(10_000)
            runCurrent()

            expectNoEvents()
            verify(megaApiGateway, times(3)).getWaitingReason()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals reports not waiting when a read fails`() = runTest {
        failRead = true
        waitingReason = 1

        createUnderTest().monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            failRead = false
            advanceTimeBy(5_000)
            runCurrent()

            assertThat(awaitItem()).isEqualTo(waiting)
            verify(megaApiGateway, times(2)).getWaitingReason()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals keeps polling when the global updates fail`() =
        runTest {
            val failingUpdates: Flow<GlobalUpdate> = flow { throw RuntimeException("updates failed") }
            whenever(megaApiGateway.globalUpdates).thenReturn(failingUpdates)

            createUnderTest().monitorSyncEngineSignals().test {
                runCurrent()
                assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
                assertThat(currentTime).isEqualTo(0)

                waitingReason = 1
                advanceTimeBy(5_000)
                runCurrent()

                assertThat(awaitItem()).isEqualTo(waiting)
                assertThat(currentTime).isEqualTo(5_000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that two collectors share one poll`() = runTest {
        val underTest = createUnderTest()

        turbineScope {
            val first = underTest.monitorSyncEngineSignals().testIn(backgroundScope)
            val second = underTest.monitorSyncEngineSignals().testIn(backgroundScope)
            runCurrent()
            assertThat(first.awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
            assertThat(second.awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            waitingReason = 1
            advanceTimeBy(5_000)
            runCurrent()

            assertThat(first.awaitItem()).isEqualTo(waiting)
            assertThat(second.awaitItem()).isEqualTo(waiting)
            verify(megaApiGateway, times(2)).getWaitingReason()
            verify(syncGateway, times(2)).isSyncStalled()
            first.cancelAndIgnoreRemainingEvents()
            second.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a late collector gets the latest value at once`() = runTest {
        val underTest = createUnderTest()

        turbineScope {
            val first = underTest.monitorSyncEngineSignals().testIn(backgroundScope)
            runCurrent()
            assertThat(first.awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)

            waitingReason = 1
            advanceTimeBy(2_000)
            runCurrent()
            val late = underTest.monitorSyncEngineSignals().testIn(backgroundScope)
            runCurrent()

            assertThat(late.awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
            assertThat(currentTime).isEqualTo(2_000)
            verify(megaApiGateway, times(1)).getWaitingReason()
            first.cancelAndIgnoreRemainingEvents()
            late.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorSyncEngineSignals stops polling after the last collector leaves`() =
        runTest {
            createUnderTest().monitorSyncEngineSignals().test {
                runCurrent()
                assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
                advanceTimeBy(4_500)
                runCurrent()
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(4_999)
            runCurrent()
            // A read 1 ms before the stop timeout proves the poll did not stop early.
            globalUpdates.emit(GlobalUpdate.OnGlobalSyncStateChanged)
            runCurrent()
            assertThat(currentTime).isEqualTo(9_499)
            verify(megaApiGateway, times(3)).getWaitingReason()

            advanceTimeBy(500)
            runCurrent()
            verify(megaApiGateway, times(3)).getWaitingReason()

            advanceTimeBy(50_001)
            runCurrent()
            assertThat(currentTime).isEqualTo(60_000)
            verify(megaApiGateway, times(3)).getWaitingReason()
        }

    @Test
    fun `test that a collector after the poll stopped never sees a stale value`() = runTest {
        val underTest = createUnderTest()
        waitingReason = 1

        underTest.monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(waiting)
            cancelAndIgnoreRemainingEvents()
        }

        advanceTimeBy(5_000)
        runCurrent()
        waitingReason = 0

        underTest.monitorSyncEngineSignals().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(SyncEngineSignals.NotWaiting)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * The background scope and the io dispatcher share one scheduler, so only a marker set while
 * this dispatcher runs a block shows which of them a read ran on.
 */
@OptIn(InternalCoroutinesApi::class)
private class MarkingDispatcher(
    private val delegate: TestDispatcher,
    private val marker: ThreadLocal<Boolean>,
) : CoroutineDispatcher(), Delay by delegate {
    override fun dispatch(context: CoroutineContext, block: Runnable) =
        delegate.dispatch(context) {
            marker.set(true)
            try {
                block.run()
            } finally {
                marker.set(false)
            }
        }
}
