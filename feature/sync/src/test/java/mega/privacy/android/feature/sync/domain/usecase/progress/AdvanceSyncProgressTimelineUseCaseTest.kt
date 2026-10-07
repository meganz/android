package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseReasonInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressTimeline
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
internal class AdvanceSyncProgressTimelineUseCaseTest {

    private lateinit var underTest: AdvanceSyncProgressTimelineUseCase

    @BeforeAll
    fun setUp() {
        underTest = AdvanceSyncProgressTimelineUseCase(SyncProgressPhaseCalculator())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("idleStarts")
    fun `test that invoke sets the idle start`(
        case: String,
        previous: SyncProgressTimeline?,
        input: SyncProgressInput,
        expectedIdleStart: Long?,
    ) {
        val result = underTest(previous, input, NOW)

        assertWithMessage(case).that(result.idleStartMillis).isEqualTo(expectedIdleStart)
    }

    private fun idleStarts(): Stream<Arguments> = Stream.of(
        Arguments.of("not seeded", busy(), input(SyncActivity.NotSeeded), null),
        Arguments.of("busy", idleSince(1_000), input(transferring()), null),
        Arguments.of("paused after busy", busy(), input(paused()), null),
        Arguments.of("no syncs after busy", busy(), input(activity()), null),
        Arguments.of("idle cold start", null, input(idle()), null),
        Arguments.of("idle after busy", busy(), input(idle()), NOW),
        Arguments.of(
            "idle after a checking that ended at the cap",
            busy(input(activity(runningWithoutStats(1, since = NOW - 13_000))), NOW - 3_000),
            input(idle()),
            NOW - 3_000,
        ),
        Arguments.of(
            "idle before the cap of a checking",
            busy(input(activity(runningWithoutStats(1, since = NOW - 3_000))), NOW + 7_000),
            input(idle()),
            NOW,
        ),
        Arguments.of(
            "idle after a checking that ended at the cap in a pause",
            busy(
                input(
                    activity(
                        runningWithoutStats(1, since = NOW - 11_000),
                        inState(2, SyncRunState.Suspended),
                        interrupted = 3,
                    )
                ),
                busyUntil = NOW - 1_000,
            ),
            input(idle()),
            null,
        ),
        Arguments.of("idle after idle", idleSince(1_000), input(idle()), 1_000L),
        Arguments.of("idle after an idle cold start", idleSince(null), input(idle()), null),
        Arguments.of(
            "idle after a checking that ended at the last of several caps",
            busy(
                input(
                    activity(
                        runningWithoutStats(1, since = NOW - 16_000),
                        runningWithoutStats(2, since = NOW - 11_000),
                        processed = 10,
                    )
                ),
                busyUntil = NOW - 6_000,
            ),
            input(idle()),
            NOW - 1_000,
        ),
        Arguments.of(
            "idle after a checking that ended at the last of several caps in a pause",
            busy(
                input(
                    activity(
                        runningWithoutStats(1, since = NOW - 16_000),
                        runningWithoutStats(2, since = NOW - 11_000),
                        inState(3, SyncRunState.Suspended),
                        processed = 10,
                        interrupted = 3,
                    )
                ),
                busyUntil = NOW - 6_000,
            ),
            input(idle()),
            null,
        ),
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource("busyUntils")
    fun `test that invoke sets busyUntilMillis only when busy without work`(
        case: String,
        input: SyncProgressInput,
        expectedIsBusy: Boolean,
        expectedBusyUntil: Long?,
    ) {
        val result = underTest(null, input, NOW)

        assertWithMessage(case).that(result.isBusy).isEqualTo(expectedIsBusy)
        assertWithMessage(case).that(result.busyUntilMillis).isEqualTo(expectedBusyUntil)
    }

    private fun busyUntils(): Stream<Arguments> = Stream.of(
        Arguments.of(
            "checking held by a sync without stats",
            input(activity(runningWithoutStats(1, since = NOW))),
            true,
            NOW + 10_000,
        ),
        Arguments.of(
            "checking while a scanning sync also waits for stats",
            input(activity(running(1, scanning = true), runningWithoutStats(2, since = NOW))),
            true,
            null,
        ),
        Arguments.of("checking by a scanning sync", input(activity(running(1, scanning = true))), true, null),
        Arguments.of("transferring", input(transferring()), true, null),
        Arguments.of("idle", input(idle()), false, null),
        Arguments.of("paused", input(paused()), false, null),
    )

    @Test
    fun `test that invoke keeps the input`() {
        val input = input(idle(), SyncPauseReasonInput.Known(SyncPauseReason.NoWifi))

        assertThat(underTest(busy(), input, NOW).input).isEqualTo(input)
    }

    private fun busy(
        input: SyncProgressInput = input(transferring()),
        busyUntil: Long? = null,
    ) = SyncProgressTimeline(
        input = input,
        isBusy = true,
        busyUntilMillis = busyUntil,
        idleStartMillis = null,
    )

    private fun idleSince(idleStart: Long?) = SyncProgressTimeline(
        input = input(idle()),
        isBusy = false,
        busyUntilMillis = null,
        idleStartMillis = idleStart,
    )

    private fun input(
        activity: SyncActivity,
        reason: SyncPauseReasonInput = SyncPauseReasonInput.Known(null),
    ) = SyncProgressInput(activity, reason)

    private fun transferring() = activity(running(1, pending = 5), processed = 5, pending = 5)

    private fun idle() = activity(running(1), processed = 10)

    private fun paused() = activity(inState(1, SyncRunState.Suspended), processed = 5, interrupted = 5)

    private fun activity(
        vararg syncs: SyncWorkState,
        processed: Int = 0,
        pending: Int = 0,
        interrupted: Int = 0,
    ) = SyncActivity(
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
    }
}
