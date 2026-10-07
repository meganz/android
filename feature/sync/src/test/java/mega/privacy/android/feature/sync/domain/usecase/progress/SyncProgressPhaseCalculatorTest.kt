package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.entity.progress.SyncEpisodeProgress
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncProgressPhaseCalculatorTest {

    private val underTest = SyncProgressPhaseCalculator()

    @Test
    fun `test that calculate returns NoSyncs when no sync is configured`() {
        val result = underTest.calculate(activity(), pauseReason = null, nowMillis = 0)

        assertThat(result.phase).isEqualTo(SyncProgressPhase.NoSyncs)
    }

    @Test
    fun `test that calculate returns UpToDate when the activity is not seeded`() {
        val result = underTest.calculate(SyncActivity.NotSeeded, pauseReason = null, nowMillis = 0)

        assertThat(result).isEqualTo(
            SyncProgressPhaseCalculator.Result(SyncProgressPhase.UpToDate, reevaluateAtMillis = null)
        )
    }

    @Test
    fun `test that calculate returns Transferring when a running sync has pending items`() {
        val result = underTest.calculate(
            activity(running(1, pending = 3), pending = 3),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Transferring)
    }

    @Test
    fun `test that calculate returns Transferring when pending items remain while another sync is suspended`() {
        val result = underTest.calculate(
            activity(running(1, pending = 3), inState(2, SyncRunState.Suspended), pending = 3),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Transferring)
    }

    @Test
    fun `test that calculate returns Checking when a running sync is scanning with nothing pending`() {
        val result = underTest.calculate(
            activity(running(1, scanning = true)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result).isEqualTo(
            SyncProgressPhaseCalculator.Result(SyncProgressPhase.Checking, reevaluateAtMillis = null)
        )
    }

    @Test
    fun `test that calculate returns Checking when a sync is loading`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Loading)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Checking)
    }

    @Test
    fun `test that calculate returns Checking when a sync is pending`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Pending)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Checking)
    }

    @Test
    fun `test that calculate returns Checking while a sync that just entered running has no stats`() {
        val result = underTest.calculate(
            activity(runningWithoutStats(1, since = 0)),
            pauseReason = null,
            nowMillis = 9_999,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Checking)
    }

    @Test
    fun `test that calculate returns a re-evaluation time at the unknown stats cap`() {
        val result = underTest.calculate(
            activity(runningWithoutStats(1, since = 1_000)),
            pauseReason = null,
            nowMillis = 2_000,
        )

        assertThat(result.reevaluateAtMillis).isEqualTo(11_000)
    }

    @Test
    fun `test that calculate returns the earliest re-evaluation time among syncs without stats`() {
        val result = underTest.calculate(
            activity(runningWithoutStats(1, since = 4_000), runningWithoutStats(2, since = 1_000)),
            pauseReason = null,
            nowMillis = 5_000,
        )

        assertThat(result.reevaluateAtMillis).isEqualTo(11_000)
    }

    @Test
    fun `test that calculate does not return Checking once the unknown stats cap has passed`() {
        val result = underTest.calculate(
            activity(runningWithoutStats(1, since = 0)),
            pauseReason = null,
            nowMillis = 10_000,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.UpToDate)
    }

    @Test
    fun `test that calculate ignores a running sync without stats whose entry time is unknown`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Running)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.UpToDate)
    }

    @Test
    fun `test that calculate returns UpToDate when a running sync is only syncing`() {
        val result = underTest.calculate(
            activity(running(1, syncing = true)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.UpToDate)
    }

    @Test
    fun `test that calculate returns Paused with StorageOverQuota when a suspended sync is over quota`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Suspended, overQuota = true)),
            pauseReason = SyncPauseReason.NoWifi,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.StorageOverQuota))
    }

    @Test
    fun `test that calculate returns Paused with the condition when a pause reason exists`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Suspended)),
            pauseReason = SyncPauseReason.LowBattery,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(
            SyncProgressPhase.Paused(SyncPauseCause.Condition(SyncPauseReason.LowBattery))
        )
    }

    @Test
    fun `test that calculate returns Paused with User when there is no pause reason`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Suspended)),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))
    }

    @Test
    fun `test that calculate returns Paused with User when every sync is suspended by pause all`() {
        val result = underTest.calculate(
            activity(
                inState(1, SyncRunState.Suspended),
                inState(2, SyncRunState.Suspended),
                interrupted = 9,
            ),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result).isEqualTo(
            SyncProgressPhaseCalculator.Result(
                SyncProgressPhase.Paused(SyncPauseCause.User),
                reevaluateAtMillis = null,
            )
        )
    }

    @Test
    fun `test that calculate returns Paused with the condition when pause all and a condition both hold`() {
        val result = underTest.calculate(
            activity(inState(1, SyncRunState.Suspended), interrupted = 4),
            pauseReason = SyncPauseReason.NotCharging,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(
            SyncProgressPhase.Paused(SyncPauseCause.Condition(SyncPauseReason.NotCharging))
        )
    }

    @Test
    fun `test that calculate returns Paused when a suspended sync holds interrupted items and another sync is idle`() {
        val result = underTest.calculate(
            activity(running(1), inState(2, SyncRunState.Suspended), interrupted = 6),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))
    }

    @Test
    fun `test that calculate returns UpToDate when a sync is paused with nothing interrupted and others are idle`() {
        val result = underTest.calculate(
            activity(running(1), inState(2, SyncRunState.Suspended), interrupted = 0),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.UpToDate)
    }

    @Test
    fun `test that calculate returns Paused with User when only a sync that is not suspended is over quota`() {
        val result = underTest.calculate(
            activity(
                inState(1, SyncRunState.Running, overQuota = true),
                inState(2, SyncRunState.Suspended),
                interrupted = 6,
            ),
            pauseReason = null,
            nowMillis = 0,
        )

        assertThat(result.phase).isEqualTo(SyncProgressPhase.Paused(SyncPauseCause.User))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonCheckingPhases")
    fun `test that calculate returns no re-evaluation time when the phase is not Checking`(
        expectedPhase: SyncProgressPhase,
        activity: SyncActivity,
    ) {
        val result = underTest.calculate(activity, pauseReason = null, nowMillis = 0)

        assertThat(result).isEqualTo(
            SyncProgressPhaseCalculator.Result(expectedPhase, reevaluateAtMillis = null)
        )
    }

    private fun nonCheckingPhases(): Stream<Arguments> = Stream.of(
        Arguments.of(SyncProgressPhase.NoSyncs, activity()),
        Arguments.of(
            SyncProgressPhase.Transferring,
            activity(running(1, pending = 2), pending = 2),
        ),
        Arguments.of(
            SyncProgressPhase.Paused(SyncPauseCause.User),
            activity(inState(1, SyncRunState.Suspended), interrupted = 3),
        ),
        Arguments.of(SyncProgressPhase.UpToDate, activity(running(1))),
        Arguments.of(SyncProgressPhase.UpToDate, activity(inState(1, SyncRunState.Disabled))),
    )

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

    private fun inState(id: Long, state: SyncRunState, overQuota: Boolean = false) = SyncWorkState(
        syncId = id,
        runState = state,
        isStorageOverQuota = overQuota,
        stats = null,
        runningSinceMillis = null,
    )
}
