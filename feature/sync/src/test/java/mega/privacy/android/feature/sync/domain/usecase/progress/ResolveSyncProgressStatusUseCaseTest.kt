package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseReasonInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStep
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressTimeline
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveSyncProgressStatusUseCaseTest {

    private lateinit var underTest: ResolveSyncProgressStatusUseCase

    @BeforeAll
    fun setUp() {
        underTest = ResolveSyncProgressStatusUseCase(GetSyncProgressPhaseUseCase())
    }

    @Test
    fun `test that invoke returns a not seeded UpToDate status without a wake when not seeded`() {
        val progress = SyncEpisodeProgress(1, 3, 2, 0, fraction = null)
        val timeline = timeline(SyncRunSnapshot.NotSeeded.copy(progress = progress), isBusy = true)

        assertThat(underTest(timeline, NOW)).isEqualTo(
            SyncProgressStep(
                status = SyncProgressStatus(
                    isSeeded = false,
                    phase = SyncProgressPhase.UpToDate,
                    progress = progress,
                ),
                timeline = timeline,
                wakeAtMillis = null,
            )
        )
    }

    @Test
    fun `test that invoke returns no status when paused before the pause reason arrives`() {
        val timeline = timeline(paused(), reason = SyncPauseReasonInput.Unknown)

        assertThat(underTest(timeline, NOW)).isEqualTo(SyncProgressStep(null, timeline, null))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pauseReasons")
    fun `test that invoke returns Paused without a wake when the pause reason is known`(
        reason: SyncPauseReason?,
        expectedCause: SyncPauseCause,
    ) {
        val timeline = timeline(paused(), reason = SyncPauseReasonInput.Known(reason))

        assertThat(underTest(timeline, NOW)).isEqualTo(
            SyncProgressStep(status(SyncProgressPhase.Paused(expectedCause), paused()), timeline, null)
        )
    }

    private fun pauseReasons(): Stream<Arguments> = Stream.of(
        Arguments.of(null, SyncPauseCause.User),
        Arguments.of(SyncPauseReason.NoWifi, SyncPauseCause.Condition(SyncPauseReason.NoWifi)),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonIdlePhases")
    fun `test that invoke returns a non idle phase with the re-evaluation time as the wake`(
        case: String,
        runSnapshot: SyncRunSnapshot,
        expectedPhase: SyncProgressPhase,
        expectedWakeAt: Long?,
    ) {
        val timeline = timeline(runSnapshot, isBusy = true)

        val result = underTest(timeline, NOW)

        assertWithMessage(case).that(result)
            .isEqualTo(SyncProgressStep(status(expectedPhase, runSnapshot), timeline, expectedWakeAt))
    }

    private fun nonIdlePhases(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "checking held by a sync without stats",
            runSnapshot(runningWithoutStats(1, since = NOW - 4_000)),
            SyncProgressPhase.Checking,
            NOW + 6_000,
        ),
        Arguments.of(
            "checking by a scanning sync",
            runSnapshot(running(1, scanning = true)),
            SyncProgressPhase.Checking,
            null,
        ),
        Arguments.of("transferring", transferring(), SyncProgressPhase.Transferring, null),
        Arguments.of("no syncs", runSnapshot(), SyncProgressPhase.NoSyncs, null),
    )

    @Test
    fun `test that invoke returns UpToDate without a wake when there is no idle start`() {
        val timeline = timeline(idle(), isBusy = false, idleStart = null)

        assertThat(underTest(timeline, NOW))
            .isEqualTo(SyncProgressStep(status(SyncProgressPhase.UpToDate, idle()), timeline, null))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("idleStretch")
    fun `test that invoke debounces then holds Completed after a busy phase`(
        case: String,
        processed: Int,
        elapsed: Long,
        expectedPhase: SyncProgressPhase?,
        expectedWakeAfterIdleStart: Long?,
    ) {
        val runSnapshot = idle(processed)
        val timeline = timeline(runSnapshot, idleStart = IDLE_START)

        val result = underTest(timeline, IDLE_START + elapsed)

        assertWithMessage(case).that(result).isEqualTo(
            SyncProgressStep(
                status = expectedPhase?.let { status(it, runSnapshot) },
                timeline = timeline,
                wakeAtMillis = expectedWakeAfterIdleStart?.let { IDLE_START + it },
            )
        )
    }

    private fun idleStretch(): Stream<Arguments> = Stream.of(
        Arguments.of("at the idle start", 10, 0L, null, 2_000L),
        Arguments.of("inside the debounce", 10, 1_999L, null, 2_000L),
        Arguments.of("at the debounce end", 10, 2_000L, SyncProgressPhase.Completed, 6_000L),
        Arguments.of("inside the hold", 10, 5_999L, SyncProgressPhase.Completed, 6_000L),
        Arguments.of("at the hold end", 10, 6_000L, SyncProgressPhase.UpToDate, null),
        Arguments.of("nothing processed after the debounce", 0, 2_000L, SyncProgressPhase.UpToDate, null),
        Arguments.of("nothing processed inside the debounce", 0, 1_000L, null, 2_000L),
    )

    @Test
    fun `test that invoke starts the idle stretch now when a busy phase ended without an input`() {
        val runSnapshot = runSnapshot(runningWithoutStats(1, since = NOW - 10_000), processed = 4)
        val timeline = timeline(runSnapshot, isBusy = true, idleStart = null)

        assertThat(underTest(timeline, NOW)).isEqualTo(
            SyncProgressStep(
                status = null,
                timeline = timeline.copy(idleStartMillis = NOW),
                wakeAtMillis = NOW + 2_000,
            )
        )
    }

    @Test
    fun `test that invoke keeps an earlier idle start when a busy phase ended without an input`() {
        val timeline = timeline(idle(), isBusy = true, idleStart = NOW - 500)

        assertThat(underTest(timeline, NOW))
            .isEqualTo(SyncProgressStep(null, timeline, NOW + 1_500))
    }

    private fun timeline(
        runSnapshot: SyncRunSnapshot,
        isBusy: Boolean = false,
        idleStart: Long? = null,
        reason: SyncPauseReasonInput = SyncPauseReasonInput.Known(null),
    ) = SyncProgressTimeline(
        input = SyncProgressInput(runSnapshot, reason),
        isBusy = isBusy,
        busyUntilMillis = null,
        idleStartMillis = idleStart,
    )

    private fun status(phase: SyncProgressPhase, runSnapshot: SyncRunSnapshot) = SyncProgressStatus(
        isSeeded = true,
        phase = phase,
        progress = runSnapshot.progress,
    )

    private fun transferring() = runSnapshot(running(1, pending = 5), processed = 5, pending = 5)

    private fun idle(processed: Int = 10) = runSnapshot(running(1), processed = processed)

    private fun paused() = runSnapshot(inState(1, SyncRunState.Suspended), processed = 5, interrupted = 5)

    private fun runSnapshot(
        vararg syncs: SyncWorkState,
        processed: Int = 0,
        pending: Int = 0,
        interrupted: Int = 0,
    ) = SyncRunSnapshot(
        isSeeded = true,
        syncs = syncs.toList(),
        progress = SyncEpisodeProgress(0, processed, pending, interrupted, fraction = null),
    )

    private fun running(id: Long, pending: Int = 0, scanning: Boolean = false) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = SyncStatsSample(
            isScanning = scanning,
            isSyncing = false,
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

    private companion object {
        const val NOW = 100_000L
        const val IDLE_START = 50_000L
    }
}
