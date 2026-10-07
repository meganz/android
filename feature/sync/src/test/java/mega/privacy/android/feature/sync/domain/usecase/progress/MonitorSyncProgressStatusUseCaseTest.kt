package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncPauseReasonUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncProgressStatusUseCaseTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }

    private val scheduler get() = extension.testDispatcher.scheduler

    private lateinit var underTest: MonitorSyncProgressStatusUseCase

    private val monitorSyncActivityUseCase = mock<MonitorSyncActivityUseCase>()
    private val monitorSyncPauseReasonUseCase = mock<MonitorSyncPauseReasonUseCase>()

    @BeforeAll
    fun setUp() {
        val calculator = SyncProgressPhaseCalculator()
        underTest = MonitorSyncProgressStatusUseCase(
            monitorSyncActivityUseCase = monitorSyncActivityUseCase,
            monitorSyncPauseReasonUseCase = monitorSyncPauseReasonUseCase,
            advanceSyncProgressTimelineUseCase = AdvanceSyncProgressTimelineUseCase(calculator),
            resolveSyncProgressStatusUseCase = ResolveSyncProgressStatusUseCase(calculator),
            currentTimeProvider = { scheduler.currentTime },
            defaultDispatcher = extension.testDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorSyncActivityUseCase, monitorSyncPauseReasonUseCase)
    }

    @Test
    fun `test that invoke emits UpToDate immediately on an idle cold start`() = runTest {
        val t0 = testScheduler.currentTime
        stub(MutableStateFlow(idle()))

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
            assertThat(testScheduler.currentTime - t0).isEqualTo(0)
        }
    }

    @Test
    fun `test that invoke reports isSeeded from the activity`() = runTest {
        stub(MutableStateFlow(SyncActivity.NotSeeded))

        underTest().test {
            runCurrent()
            val status = awaitItem()
            assertThat(status.isSeeded).isFalse()
            assertThat(status.phase).isEqualTo(SyncProgressPhase.UpToDate)
        }
    }

    @Test
    fun `test that invoke emits a not seeded status without waiting for the pause reason`() =
        runTest {
            val t0 = testScheduler.currentTime
            stub(MutableStateFlow(SyncActivity.NotSeeded), MutableSharedFlow())

            underTest().test {
                runCurrent()
                assertThat(awaitItem()).isEqualTo(
                    SyncProgressStatus(
                        isSeeded = false,
                        phase = SyncProgressPhase.UpToDate,
                        progress = SyncEpisodeProgress.None,
                    )
                )
                assertThat(testScheduler.currentTime - t0).isEqualTo(0)
            }
        }

    @Test
    fun `test that invoke emits a not seeded status at once after transferring`() = runTest {
        val t0 = testScheduler.currentTime
        val activityFlow = MutableStateFlow(transferring())
        stub(activityFlow)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

            advanceTimeBy(1_000)
            activityFlow.value = SyncActivity.NotSeeded
            runCurrent()
            assertThat(awaitItem().isSeeded).isFalse()
            assertThat(testScheduler.currentTime - t0).isEqualTo(1_000)

            advanceTimeBy(9_000)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke does not restart the idle debounce when the pause reason repeats`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(transferring())
            val reason = MutableSharedFlow<SyncPauseReason?>()
            stub(activityFlow, reason)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)
                reason.emit(null)
                runCurrent()

                advanceTimeBy(10_000)
                activityFlow.value = idle()
                runCurrent()
                advanceTimeBy(1_000)
                reason.emit(null)
                runCurrent()
                advanceTimeBy(999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Completed)
                assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)
            }
        }

    @Test
    fun `test that invoke emits Transferring while pending items remain`() = runTest {
        val transferring = transferring()
        stub(MutableStateFlow(transferring))

        underTest().test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                SyncProgressStatus(
                    isSeeded = true,
                    phase = SyncProgressPhase.Transferring,
                    progress = transferring.progress,
                )
            )
        }
    }

    @Test
    fun `test that invoke emits Completed after activity stays idle for the idle debounce`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(transferring())
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(10_000)
                activityFlow.value = idle()
                runCurrent()
                advanceTimeBy(1_999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                val status = awaitItem()
                assertThat(status.phase).isEqualTo(SyncProgressPhase.Completed)
                assertThat(status.progress.processedItems).isEqualTo(10)
                assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke does not emit Completed when work returns within the idle debounce`() =
        runTest {
            val activityFlow = MutableStateFlow(transferring())
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(10_000)
                activityFlow.value = idle()
                runCurrent()
                advanceTimeBy(1_500)
                activityFlow.value = transferring(processed = 10, pending = 3)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(8_500)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke emits UpToDate after the completed hold`() = runTest {
        val t0 = testScheduler.currentTime
        val activityFlow = MutableStateFlow(transferring())
        stub(activityFlow)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

            advanceTimeBy(10_000)
            activityFlow.value = idle()
            advanceTimeBy(2_000)
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Completed)
            assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)

            advanceTimeBy(3_999)
            expectNoEvents()
            advanceTimeBy(1)
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
            assertThat(testScheduler.currentTime - t0).isEqualTo(16_000)
        }
    }

    @Test
    fun `test that invoke emits UpToDate without Completed when nothing was processed`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(activity(running(1, scanning = true)))
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(1_000)
                activityFlow.value = idle(processed = 0)
                runCurrent()
                advanceTimeBy(1_999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
                assertThat(testScheduler.currentTime - t0).isEqualTo(3_000)

                advanceTimeBy(10_000)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke keeps the idle debounce running when activity changes while idle`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(transferring())
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(10_000)
                activityFlow.value = idle()
                runCurrent()
                advanceTimeBy(1_000)
                activityFlow.value = activity(running(1, syncing = true), processed = 10)
                runCurrent()
                advanceTimeBy(999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Completed)
                assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that invoke does not emit Completed again for later idle updates`() = runTest {
        val activityFlow = MutableStateFlow(transferring())
        stub(activityFlow)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

            advanceTimeBy(10_000)
            activityFlow.value = idle()
            advanceTimeBy(6_000)
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Completed)
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)

            advanceTimeBy(4_000)
            activityFlow.value = activity(running(1, syncing = true), processed = 10)
            runCurrent()
            advanceTimeBy(10_000)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke re-evaluates when the unknown stats cap elapses`() = runTest {
        val t0 = testScheduler.currentTime
        stub(MutableStateFlow(activity(runningWithoutStats(1, since = t0))))

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

            advanceTimeBy(11_999)
            expectNoEvents()

            advanceTimeBy(1)
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
            assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)
        }
    }

    @Test
    fun `test that invoke starts the idle debounce at the unknown stats cap when a later idle input arrives`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(activity(runningWithoutStats(1, since = t0)))
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(11_000)
                activityFlow.value = activity(running(1))
                runCurrent()
                expectNoEvents()

                advanceTimeBy(999)
                expectNoEvents()
                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
                assertThat(testScheduler.currentTime - t0).isEqualTo(12_000)
            }
        }

    @Test
    fun `test that invoke emits Paused without the idle debounce when syncs are paused mid transfer`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(transferring())
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(10_000)
                activityFlow.value = activity(
                    inState(1, SyncRunState.Suspended),
                    processed = 5,
                    interrupted = 5,
                )
                runCurrent()
                assertThat(awaitItem().phase)
                    .isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))
                assertThat(testScheduler.currentTime - t0).isEqualTo(10_000)

                advanceTimeBy(10_000)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke reports interrupted items as files left while paused`() = runTest {
        stub(
            MutableStateFlow(activity(inState(1, SyncRunState.Suspended), interrupted = 60)),
            MutableStateFlow(SyncPauseReason.NoWifi),
        )

        underTest().test {
            runCurrent()
            val status = awaitItem()
            assertThat(status.phase).isEqualTo(
                SyncProgressPhase.Paused(SyncPauseCause.Condition(SyncPauseReason.NoWifi))
            )
            assertThat(status.filesLeft).isEqualTo(60)
        }
    }

    @Test
    fun `test that invoke emits NoSyncs at once when the last sync is removed mid transfer`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(transferring())
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

                advanceTimeBy(10_000)
                activityFlow.value = activity(processed = 5)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.NoSyncs)
                assertThat(testScheduler.currentTime - t0).isEqualTo(10_000)

                advanceTimeBy(10_000)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke waits for the pause reason before emitting a paused status`() =
        runTest {
            val reason = MutableSharedFlow<SyncPauseReason?>()
            stub(
                MutableStateFlow(activity(inState(1, SyncRunState.Suspended), interrupted = 5)),
                reason,
            )

            underTest().test {
                runCurrent()
                expectNoEvents()

                reason.emit(SyncPauseReason.NoWifi)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(
                    SyncProgressPhase.Paused(SyncPauseCause.Condition(SyncPauseReason.NoWifi))
                )
            }
        }

    @Test
    fun `test that invoke emits a non paused status before the pause reason arrives`() =
        runTest {
            val t0 = testScheduler.currentTime
            stub(MutableStateFlow(transferring()), MutableSharedFlow())

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)
                assertThat(testScheduler.currentTime - t0).isEqualTo(0)
            }
        }

    @Test
    fun `test that invoke does not emit duplicate statuses`() = runTest {
        val activityFlow = MutableSharedFlow<SyncActivity>()
        stub(activityFlow)

        underTest().test {
            runCurrent()
            activityFlow.emit(transferring())
            runCurrent()
            activityFlow.emit(transferring())
            runCurrent()

            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke emits UpToDate at once when the unknown stats cap ends in a pause`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(
                activity(
                    runningWithoutStats(1, since = t0),
                    inState(2, SyncRunState.Suspended),
                    processed = 7,
                    interrupted = 3,
                )
            )
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(10_000)
                runCurrent()
                assertThat(awaitItem().phase)
                    .isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))

                advanceTimeBy(1_000)
                activityFlow.value = activity(runningWithoutStats(1, since = t0), processed = 7)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
                assertThat(testScheduler.currentTime - t0).isEqualTo(11_000)

                advanceTimeBy(10_000)
                runCurrent()
                expectNoEvents()
            }
        }

    @Test
    fun `test that invoke emits UpToDate at once when the last of several unknown stats caps ends in a pause`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(
                activity(
                    runningWithoutStats(1, since = t0),
                    inState(3, SyncRunState.Suspended),
                    processed = 10,
                    interrupted = 3,
                )
            )
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(5_000)
                activityFlow.value = activity(
                    runningWithoutStats(1, since = t0),
                    runningWithoutStats(2, since = t0 + 5_000),
                    inState(3, SyncRunState.Suspended),
                    processed = 10,
                    interrupted = 3,
                )
                runCurrent()
                advanceTimeBy(9_999)
                runCurrent()
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase)
                    .isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))
                assertThat(testScheduler.currentTime - t0).isEqualTo(15_000)

                advanceTimeBy(500)
                activityFlow.value = activity(
                    runningWithoutStats(1, since = t0),
                    runningWithoutStats(2, since = t0 + 5_000),
                    processed = 10,
                )
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
                assertThat(testScheduler.currentTime - t0).isEqualTo(15_500)

                advanceTimeBy(10_000)
                runCurrent()
                expectNoEvents()
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("idleStretchStoppers")
    fun `test that invoke emits UpToDate at once when syncs resume after a pause during the idle debounce`(
        expectedStop: SyncProgressPhase,
        stop: SyncActivity,
    ) = runTest {
        val t0 = testScheduler.currentTime
        val activityFlow = MutableStateFlow(transferring())
        stub(activityFlow)

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Transferring)

            advanceTimeBy(10_000)
            activityFlow.value = idle()
            runCurrent()
            advanceTimeBy(1_000)
            activityFlow.value = stop
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(expectedStop)

            advanceTimeBy(500)
            activityFlow.value = idle()
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
            assertThat(testScheduler.currentTime - t0).isEqualTo(11_500)

            advanceTimeBy(10_000)
            runCurrent()
            expectNoEvents()
        }
    }

    private fun idleStretchStoppers(): Stream<Arguments> = Stream.of(
        Arguments.of(
            SyncProgressPhase.Paused(SyncPauseCause.User),
            activity(inState(1, SyncRunState.Suspended), processed = 10, interrupted = 5),
        ),
        Arguments.of(SyncProgressPhase.NoSyncs, activity(processed = 10)),
    )

    @Test
    fun `test that invoke starts the idle debounce at the idle input when it arrives before the unknown stats cap`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(activity(runningWithoutStats(1, since = t0)))
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(3_000)
                activityFlow.value = activity(running(1))
                runCurrent()
                advanceTimeBy(1_999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.UpToDate)
                assertThat(testScheduler.currentTime - t0).isEqualTo(5_000)
            }
        }

    @Test
    fun `test that invoke starts the idle debounce at the idle input when a scanning sync also waits for stats`() =
        runTest {
            val t0 = testScheduler.currentTime
            val activityFlow = MutableStateFlow(
                activity(
                    running(1, scanning = true),
                    runningWithoutStats(2, since = t0),
                    processed = 5,
                )
            )
            stub(activityFlow)

            underTest().test {
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Checking)

                advanceTimeBy(30_000)
                activityFlow.value = activity(running(1), running(2), processed = 10)
                runCurrent()
                advanceTimeBy(1_999)
                expectNoEvents()

                advanceTimeBy(1)
                runCurrent()
                assertThat(awaitItem().phase).isEqualTo(SyncProgressPhase.Completed)
                assertThat(testScheduler.currentTime - t0).isEqualTo(32_000)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonPausedPhases")
    fun `test that invoke emits a non paused phase at once before the pause reason arrives`(
        expectedPhase: SyncProgressPhase,
        activity: SyncActivity,
    ) = runTest {
        val t0 = testScheduler.currentTime
        stub(MutableStateFlow(activity), MutableSharedFlow())

        underTest().test {
            runCurrent()
            assertThat(awaitItem().phase).isEqualTo(expectedPhase)
            assertThat(testScheduler.currentTime - t0).isEqualTo(0)
        }
    }

    private fun nonPausedPhases(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncProgressPhase.Checking, activity(running(1, scanning = true))),
        Arguments.of(SyncProgressPhase.UpToDate, idle()),
        Arguments.of(SyncProgressPhase.NoSyncs, activity()),
    )

    private fun stub(
        activity: Flow<SyncActivity>,
        reason: Flow<SyncPauseReason?> = MutableStateFlow(null),
    ) {
        whenever(monitorSyncActivityUseCase()).thenReturn(activity)
        whenever(monitorSyncPauseReasonUseCase()).thenReturn(reason)
    }

    private fun transferring(processed: Int = 5, pending: Int = 5) =
        activity(running(1, pending = pending), processed = processed, pending = pending)

    private fun idle(processed: Int = 10) = activity(running(1), processed = processed)

    private fun activity(
        vararg syncs: SyncWorkState,
        isSeeded: Boolean = true,
        processed: Int = 0,
        pending: Int = 0,
        interrupted: Int = 0,
    ) = SyncActivity(
        isSeeded = isSeeded,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress(0, processed, pending, interrupted, fraction = null),
    )

    private fun running(
        id: Long,
        pending: Int = 0,
        scanning: Boolean = false,
        syncing: Boolean = false,
    ) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = SyncStatsSample(
            isScanning = scanning,
            isSyncing = syncing,
            pendingUploads = pending,
            pendingDownloads = 0,
        ),
        runningSinceMillis = null,
    )

    private fun runningWithoutStats(id: Long, since: Long) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = since,
    )

    private fun inState(id: Long, state: SyncRunState) = SyncWorkState(
        syncId = id,
        runState = state,
        isStorageOverQuota = false,
        stats = null,
        runningSinceMillis = null,
    )
}
