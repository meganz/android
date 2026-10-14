package mega.privacy.android.feature.sync.domain.usecase.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Companion.Initial
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Counter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Interruption
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncStatsSample
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.usecase.progress.AdvanceSyncProgressEpisodeUseCase.Companion.EPISODE_IDLE_GRACE
import mega.privacy.android.feature.sync.domain.usecase.progress.AdvanceSyncProgressEpisodeUseCase.Companion.INTERRUPTION_HOLD
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AdvanceSyncProgressEpisodeUseCaseTest {

    private lateinit var underTest: AdvanceSyncProgressEpisodeUseCase

    @BeforeAll
    fun setUp() {
        underTest = AdvanceSyncProgressEpisodeUseCase()
    }

    private val grace = EPISODE_IDLE_GRACE.inWholeMilliseconds
    private val hold = INTERRUPTION_HOLD.inWholeMilliseconds

    private fun running(
        id: Long,
        pending: Int,
        scanning: Boolean = false,
        syncing: Boolean = false,
        since: Long? = null,
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
        runningSinceMillis = since,
    )

    private fun runningDownloads(id: Long, pending: Int, scanning: Boolean = false) = SyncWorkState(
        syncId = id,
        runState = SyncRunState.Running,
        isStorageOverQuota = false,
        stats = SyncStatsSample(
            isScanning = scanning,
            isSyncing = false,
            pendingUploads = 0,
            pendingDownloads = pending,
        ),
        runningSinceMillis = null,
    )

    private fun runningWithoutStats(id: Long, since: Long) =
        SyncWorkState(id, SyncRunState.Running, false, null, since)

    private fun inState(id: Long, state: SyncRunState) =
        SyncWorkState(id, state, false, null, null)

    private fun SyncProgressEpisode.step(nowMillis: Long, vararg syncs: SyncWorkState) =
        underTest(this, syncs.toList(), nowMillis)

    private fun steps(vararg inputs: List<SyncWorkState>): List<SyncProgressEpisode> =
        inputs.runningFoldIndexed(Initial) { index, episode, syncs ->
            underTest(episode, syncs, index * 1_000L)
        }.drop(1)

    private fun assertFraction(actual: Float?, expected: Float) {
        assertThat(actual).isNotNull()
        assertThat(actual!!).isWithin(1e-6f).of(expected)
    }

    private fun interruptedAfterSixOfTen(): SyncProgressEpisode = Initial
        .step(0, running(1, 10))
        .step(1_000, running(1, 6))
        .step(2_000, inState(1, SyncRunState.Suspended))

    private fun suspendedAtSixtyOfHundred(): SyncProgressEpisode = Initial
        .step(0, running(1, 100))
        .step(1_000, running(1, 60))
        .step(2_000, inState(1, SyncRunState.Suspended))

    private fun fullWhileScanning(): SyncProgressEpisode = Initial
        .step(0, running(1, 10))
        .step(500, running(1, 10))
        .step(1_000, running(1, 0, scanning = true))

    /**
     * A device run with one sync: uploads finish while the sync keeps scanning, then downloads
     * arrive. Each pair is the time in ms and the sync state at that time.
     */
    private val deviceRun = listOf(
        0L to running(1, 250, scanning = true),
        1_000L to running(1, 250),
        2_000L to running(1, 500),
        3_800L to running(1, 493),
        56_800L to running(1, 1),
        57_800L to running(1, 1, scanning = true),
        58_800L to running(1, 0, scanning = true),
        71_800L to runningDownloads(1, 750),
        75_000L to runningDownloads(1, 1_000),
        518_000L to runningDownloads(1, 2),
        519_000L to runningDownloads(1, 2, scanning = true),
        532_500L to runningDownloads(1, 0, scanning = true),
        570_400L to runningDownloads(1, 0),
    )

    private fun deviceRunUntil(lastMillis: Long): Map<Long, SyncProgressEpisode> {
        val samples = deviceRun.takeWhile { (millis, _) -> millis <= lastMillis }
        val episodes = samples
            .runningFold(Initial) { episode, (millis, sync) -> episode.step(millis, sync) }
            .drop(1)
        return samples.map { (millis, _) -> millis }.zip(episodes).toMap()
    }

    @Test
    fun `test that invoke sets total to pending when a sync is first observed`() {
        val progress = Initial.step(0, running(1, 10)).toProgress()

        assertThat(progress.processedItems).isEqualTo(0)
        assertThat(progress.pendingItems).isEqualTo(10)
        assertThat(progress.totalItems).isEqualTo(10)
    }

    @Test
    fun `test that invoke adds a pending decrease to processed items`() {
        val progress = steps(listOf(running(1, 10)), listOf(running(1, 4))).last().toProgress()

        assertThat(progress.processedItems).isEqualTo(6)
        assertThat(progress.pendingItems).isEqualTo(4)
    }

    @Test
    fun `test that invoke adds a pending increase to total items without changing processed items`() {
        val progress = steps(listOf(running(1, 10)), listOf(running(1, 15))).last().toProgress()

        assertThat(progress.processedItems).isEqualTo(0)
        assertThat(progress.totalItems).isEqualTo(15)
    }

    @Test
    fun `test that invoke keeps counters of two syncs independent`() {
        val progress = steps(
            listOf(running(1, 10), running(2, 20)),
            listOf(running(1, 5), running(2, 20)),
        ).last().toProgress()

        assertThat(progress.processedItems).isEqualTo(5)
        assertThat(progress.pendingItems).isEqualTo(25)
    }

    @Test
    fun `test that invoke ignores syncs that are running without stats`() {
        val episode = Initial.step(0, runningWithoutStats(1, since = 0))

        assertThat(episode.counters).isEmpty()
        assertThat(episode.toProgress().pendingItems).isEqualTo(0)
    }

    @Test
    fun `test that invoke ignores isSyncing when there is no pending work`() {
        val first = Initial.step(0, running(1, 0, syncing = true))
        val second = first.step(grace, running(1, 2))

        assertThat(first.idleSinceMillis).isEqualTo(0L)
        assertThat(second.episodeId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke keeps processed items when a sync leaves running`() {
        val progress = steps(
            listOf(running(1, 10)),
            listOf(running(1, 4)),
            listOf(inState(1, SyncRunState.Suspended)),
        ).last().toProgress()

        assertThat(progress.processedItems).isEqualTo(6)
        assertThat(progress.pendingItems).isEqualTo(0)
    }

    @Test
    fun `test that invoke does not count work before a suspend as done after the sync resumes`() {
        val episode = suspendedAtSixtyOfHundred()
            .step(3_000, runningWithoutStats(1, since = 3_000))
            .step(4_000, running(1, 5, scanning = true))

        assertThat(episode.toProgress().processedItems).isEqualTo(40)
    }

    @Test
    fun `test that invoke treats the first stats after resume as a first observation`() {
        val episode = suspendedAtSixtyOfHundred()
            .step(3_000, runningWithoutStats(1, since = 3_000))
            .step(4_000, running(1, 60))

        assertThat(episode.counters[1L]).isEqualTo(Counter(60, 60, awaitingScanEnd = true))
        assertThat(episode.toProgress().processedItems).isEqualTo(40)
        assertThat(episode.toProgress().pendingItems).isEqualTo(60)
    }

    @Test
    fun `test that invoke holds the fraction while a restarted sync has not finished its first scan`() {
        val episodes = steps(
            listOf(running(1, 100)),
            listOf(running(1, 60)),
            listOf(inState(1, SyncRunState.Suspended)),
            listOf(runningWithoutStats(1, since = 3_000)),
            listOf(running(1, 5, scanning = true)),
            listOf(running(1, 30, scanning = true)),
            listOf(running(1, 60)),
            listOf(running(1, 50)),
        )

        episodes.subList(1, 7).forEach { assertFraction(it.fraction, 0.4f) }
        assertFraction(episodes.last().fraction, 0.5f)
    }

    @Test
    fun `test that invoke keeps the fraction while a sync is interrupted`() {
        val episode = suspendedAtSixtyOfHundred()

        assertFraction(episode.fraction, 0.4f)
        assertThat(episode.toProgress().interruptedItems).isEqualTo(60)
    }

    @Test
    fun `test that invoke keeps the fraction while a resumed sync has no stats yet`() {
        val episode = suspendedAtSixtyOfHundred()
            .step(3_000, runningWithoutStats(1, since = 3_000))

        assertFraction(episode.fraction, 0.4f)
        assertThat(episode.toProgress().interruptedItems).isEqualTo(0)
    }

    @Test
    fun `test that invoke counts interrupted items in the fraction of the syncs still running`() {
        val episode = steps(
            listOf(running(1, 50), running(2, 50)),
            listOf(running(1, 30), running(2, 50)),
            listOf(running(1, 30), inState(2, SyncRunState.Suspended)),
            listOf(running(1, 20), inState(2, SyncRunState.Suspended)),
        ).last()

        assertFraction(episode.fraction, 0.3f)
    }

    @Test
    fun `test that invoke holds the fraction when every sync has been removed`() {
        val episode = steps(
            listOf(running(1, 10)),
            listOf(running(1, 4)),
            emptyList(),
        ).last()

        assertFraction(episode.fraction, 0.6f)
        assertThat(episode.toProgress().processedItems).isEqualTo(6)
    }

    @Test
    fun `test that invoke keeps the episode open while a suspended sync has interrupted items`() {
        val suspended = Initial
            .step(0, running(1, 10))
            .step(500, running(1, 6))
            .step(1_000, inState(1, SyncRunState.Suspended))
        val stillSuspended = suspended.step(61_000, inState(1, SyncRunState.Suspended))
        val resumed = stillSuspended.step(120_000, running(1, 6))

        assertThat(suspended.idleSinceMillis).isNull()
        assertThat(stillSuspended.idleSinceMillis).isNull()
        assertThat(resumed.idleSinceMillis).isNull()
        assertThat(resumed.episodeId).isEqualTo(0L)
        assertThat(resumed.toProgress().processedItems).isEqualTo(4)
    }

    @Test
    fun `test that invoke drops an interruption older than the interruption hold`() {
        val suspendedAt = 2_000L
        val episode = interruptedAfterSixOfTen()
            .step(suspendedAt + hold, inState(1, SyncRunState.Suspended))

        assertThat(episode.interruptions).isEmpty()
        assertThat(episode.toProgress().interruptedItems).isEqualTo(0)
        assertThat(episode.idleSinceMillis).isEqualTo(suspendedAt + hold)
    }

    @Test
    fun `test that invoke does not hold an interruption for a deleted sync`() {
        val episode = steps(listOf(running(1, 10)), listOf(running(1, 6)), emptyList()).last()

        assertThat(episode.interruptions).isEmpty()
        assertThat(episode.toProgress().processedItems).isEqualTo(4)
    }

    @Test
    fun `test that invoke drops the interruption when a suspended sync is deleted`() {
        val episode = interruptedAfterSixOfTen().step(3_000)

        assertThat(episode.interruptions).isEmpty()
        assertThat(episode.toProgress().interruptedItems).isEqualTo(0)
        assertThat(episode.idleSinceMillis).isEqualTo(3_000L)
    }

    @Test
    fun `test that invoke does not record an interruption for a sync that is disabled while running`() {
        val episode = steps(
            listOf(running(1, 10)),
            listOf(running(1, 6)),
            listOf(inState(1, SyncRunState.Disabled)),
        ).last()

        assertThat(episode.interruptions).isEmpty()
        assertThat(episode.toProgress().processedItems).isEqualTo(4)
    }

    @Test
    fun `test that invoke does not record an interruption for a sync suspended with nothing pending`() {
        val episode = steps(
            listOf(running(1, 5)),
            listOf(running(1, 0)),
            listOf(inState(1, SyncRunState.Suspended)),
        ).last()

        assertThat(episode.interruptions).isEmpty()
    }

    @Test
    fun `test that invoke drops an interruption when its sync returns to running`() {
        val episode = interruptedAfterSixOfTen().step(3_000, runningWithoutStats(1, since = 3_000))

        assertThat(episode.interruptions).isEmpty()
    }

    @Test
    fun `test that invoke drops an interruption when its sync is later disabled`() {
        val episode = interruptedAfterSixOfTen().step(3_000, inState(1, SyncRunState.Disabled))

        assertThat(episode.interruptions).isEmpty()
    }

    @Test
    fun `test that invoke keeps an interruption while its sync is loading again`() {
        val episode = interruptedAfterSixOfTen().step(3_000, inState(1, SyncRunState.Loading))

        assertThat(episode.interruptions).containsExactly(1L, Interruption(6, sinceMillis = 2_000))
    }

    @Test
    fun `test that invoke continues the episode when work returns within the idle grace`() {
        val episode = Initial
            .step(0, running(1, 10))
            .step(1_000, running(1, 0))
            .step(1_000 + grace - 1, running(1, 3))

        assertThat(episode.episodeId).isEqualTo(0L)
        assertThat(episode.toProgress().processedItems).isEqualTo(10)
    }

    @Test
    fun `test that invoke starts a new episode when work returns after the idle grace`() {
        val episode = Initial
            .step(0, running(1, 10))
            .step(1_000, running(1, 0))
            .step(1_000 + grace, running(1, 3))

        assertThat(episode.episodeId).isEqualTo(1L)
        assertThat(episode.toProgress().processedItems).isEqualTo(0)
        assertThat(episode.toProgress().totalItems).isEqualTo(3)
        assertThat(episode.fraction).isNull()
    }

    @Test
    fun `test that invoke sets the idle start when there is no work`() {
        val first = Initial.step(1_000, running(1, 0))
        val second = first.step(2_000, running(1, 0))

        assertThat(first.idleSinceMillis).isEqualTo(1_000L)
        assertThat(second.idleSinceMillis).isEqualTo(1_000L)
        assertThat(second.fraction).isNull()
    }

    @Test
    fun `test that invoke clears the idle start when work returns`() {
        val episode = Initial
            .step(1_000, running(1, 0))
            .step(2_000, running(1, 0))
            .step(3_000, running(1, 1))

        assertThat(episode.idleSinceMillis).isNull()
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SyncRunState::class, names = ["Pending", "Loading"])
    fun `test that invoke counts a pending or loading sync as work`(state: SyncRunState) {
        val episode = Initial
            .step(0, inState(1, state))
            .step(grace + 1, inState(1, state))

        assertThat(episode.idleSinceMillis).isNull()
    }

    @Test
    fun `test that invoke never decreases the fraction while total items grow`() {
        val episode = steps(
            listOf(running(1, 10)),
            listOf(running(1, 5)),
            listOf(running(1, 20)),
        ).last()

        assertFraction(episode.fraction, 0.5f)
    }

    @Test
    fun `test that invoke returns an indeterminate fraction until the first determinate value`() {
        val episodes = steps(
            listOf(inState(1, SyncRunState.Loading)),
            listOf(running(1, 10, scanning = true)),
            listOf(running(1, 10)),
            listOf(running(1, 8)),
        )

        assertThat(episodes[0].fraction).isNull()
        assertThat(episodes[1].fraction).isNull()
        assertFraction(episodes[2].fraction, 0f)
        assertFraction(episodes[3].fraction, 0.2f)
    }

    @Test
    fun `test that invoke holds the fraction for one sample on a first observation with pending items`() {
        val episodes = steps(listOf(running(1, 10)), listOf(running(1, 8)))

        assertThat(episodes[0].fraction).isNull()
        assertFraction(episodes[1].fraction, 0.2f)
    }

    @Test
    fun `test that invoke does not hold the fraction for an idle sync`() {
        val episodes = steps(
            listOf(running(1, 0), running(2, 10)),
            listOf(running(1, 0), running(2, 5)),
        )

        assertThat(episodes[0].counters[1L]?.awaitingScanEnd).isFalse()
        assertFraction(episodes[1].fraction, 0.5f)
    }

    @Test
    fun `test that invoke clears awaiting scan end on the first sample that is not scanning`() {
        val episodes = steps(listOf(running(1, 5, scanning = true)), listOf(running(1, 5)))

        assertThat(episodes[0].counters[1L]?.awaitingScanEnd).isTrue()
        assertThat(episodes[1].counters[1L]?.awaitingScanEnd).isFalse()
    }

    @Test
    fun `test that invoke keeps awaiting scan end while the same scanning sample is passed again`() {
        val episode = steps(
            listOf(running(1, 5, scanning = true), running(2, 10)),
            listOf(running(1, 5, scanning = true), running(2, 8)),
        ).last()

        assertThat(episode.counters[1L]?.awaitingScanEnd).isTrue()
        assertThat(episode.fraction).isNull()
    }

    @Test
    fun `test that invoke clears a first observation hold on the next advance even when that sync sent no new sample`() {
        val episode = steps(
            listOf(running(1, 10), running(2, 10)),
            listOf(running(1, 10), running(2, 8)),
        ).last()

        assertThat(episode.counters[1L]?.awaitingScanEnd).isFalse()
        assertFraction(episode.fraction, 0.1f)
    }

    @Test
    fun `test that invoke reaches a full fraction when all pending work is done`() {
        val episode = steps(
            listOf(running(1, 10)),
            listOf(running(1, 4)),
            listOf(running(1, 0)),
        ).last()

        assertFraction(episode.fraction, 1f)
    }

    @Test
    fun `test that invoke follows the worked example in the design`() {
        val held = 80f / 140f
        val suspendedA = inState(1, SyncRunState.Suspended)
        val suspendedB = inState(2, SyncRunState.Suspended)
        var episode = Initial

        fun row(seconds: Long, vararg syncs: SyncWorkState) {
            episode = episode.step(seconds * 1_000L, *syncs)
        }

        fun assertRow(
            counters: Map<Long, Counter>,
            processed: Int,
            pending: Int,
            interrupted: Int,
            fraction: Float?,
        ) {
            val progress = episode.toProgress()
            assertThat(episode.counters).isEqualTo(counters)
            assertThat(progress.processedItems).isEqualTo(processed)
            assertThat(progress.pendingItems).isEqualTo(pending)
            assertThat(progress.interruptedItems).isEqualTo(interrupted)
            if (fraction == null) assertThat(progress.fraction).isNull()
            else assertFraction(progress.fraction, fraction)
        }

        row(0, running(1, 100))
        assertRow(mapOf(1L to Counter(100, 100, true)), 0, 100, 0, null)

        row(6, running(1, 80))
        assertRow(mapOf(1L to Counter(100, 80, false)), 20, 80, 0, 0.2f)

        row(12, running(1, 120, scanning = true))
        assertRow(mapOf(1L to Counter(140, 120, false)), 20, 120, 0, 0.2f)

        row(18, running(1, 60))
        assertRow(mapOf(1L to Counter(140, 60, false)), 80, 60, 0, held)

        row(20, running(1, 60), runningWithoutStats(2, since = 20_000))
        assertRow(mapOf(1L to Counter(140, 60, false)), 80, 60, 0, held)

        row(22, running(1, 60), running(2, 0, scanning = true))
        assertRow(
            mapOf(1L to Counter(140, 60, false), 2L to Counter(0, 0, true)), 80, 60, 0, held,
        )

        row(28, running(1, 60), running(2, 10))
        assertRow(
            mapOf(1L to Counter(140, 60, false), 2L to Counter(10, 10, false)), 80, 70, 0, held,
        )

        row(30, suspendedA, running(2, 10))
        row(30, suspendedA, suspendedB)
        assertRow(emptyMap(), 80, 0, 70, held)
        assertThat(episode.retiredProcessed).isEqualTo(80)
        assertThat(episode.interruptions).containsExactly(
            1L, Interruption(60, 30_000),
            2L, Interruption(10, 30_000),
        )

        row(90, runningWithoutStats(1, since = 90_000), runningWithoutStats(2, since = 90_000))
        assertRow(emptyMap(), 80, 0, 0, held)
        assertThat(episode.interruptions).isEmpty()
        assertThat(episode.idleSinceMillis).isEqualTo(90_000L)

        row(93, running(1, 3, scanning = true), runningWithoutStats(2, since = 90_000))
        assertRow(mapOf(1L to Counter(3, 3, true)), 80, 3, 0, held)
        assertThat(episode.episodeId).isEqualTo(0L)

        row(95, running(1, 3, scanning = true), running(2, 10))
        assertRow(
            mapOf(1L to Counter(3, 3, true), 2L to Counter(10, 10, true)), 80, 13, 0, held,
        )

        row(99, running(1, 65), running(2, 10))
        assertRow(
            mapOf(1L to Counter(65, 65, false), 2L to Counter(10, 10, false)), 80, 75, 0, held,
        )

        row(101, running(1, 65), running(2, 8))
        assertRow(
            mapOf(1L to Counter(65, 65, false), 2L to Counter(10, 8, false)), 82, 73, 0, held,
        )

        row(400, running(1, 0), running(2, 8))
        row(400, running(1, 0), running(2, 0))
        assertRow(
            mapOf(1L to Counter(65, 0, false), 2L to Counter(10, 0, false)), 155, 0, 0, 1f,
        )
        assertThat(episode.idleSinceMillis).isEqualTo(400_000L)

        row(1_000, running(1, 0, scanning = true), running(2, 0))
        assertRow(mapOf(1L to Counter(0, 0, true), 2L to Counter(0, 0, false)), 0, 0, 0, null)
        assertThat(episode.episodeId).isEqualTo(1L)

        row(1_003, running(1, 1), running(2, 0))
        assertRow(mapOf(1L to Counter(1, 1, false), 2L to Counter(0, 0, false)), 0, 1, 0, 0f)
    }

    @Test
    fun `test that invoke starts a new episode when files arrive the idle grace after the episode became full while a sync kept scanning`() {
        val episodes = deviceRunUntil(71_800)
        val full = episodes.getValue(58_800)
        val joined = episodes.getValue(71_800)

        assertFraction(full.fraction, 1f)
        assertThat(full.fullSinceMillis).isEqualTo(58_800L)
        assertThat(full.idleSinceMillis).isNull()
        assertThat(joined.episodeId).isEqualTo(1L)
        assertThat(joined.toProgress().processedItems).isEqualTo(0)
        assertThat(joined.toProgress().pendingItems).isEqualTo(750)
        assertThat(joined.fraction).isNull()
    }

    @Test
    fun `test that invoke starts a new episode when files arrive exactly the idle grace after the episode became full`() {
        val episode = fullWhileScanning().step(1_000 + grace, running(1, 3))

        assertThat(episode.episodeId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke starts a new episode when files arrive the idle grace after the episode became full and within the idle grace after it went idle`() {
        val episode = fullWhileScanning()
            .step(4_200, running(1, 0))
            .step(7_300, running(1, 3))

        assertThat(episode.episodeId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke starts a new episode when a scanning sync reports files the idle grace after the episode became full`() {
        val episode = fullWhileScanning().step(1_000 + grace + 300, running(1, 3, scanning = true))

        assertThat(episode.episodeId).isEqualTo(1L)
        assertThat(episode.fraction).isNull()
    }

    @Test
    fun `test that invoke continues a full episode when files arrive within the idle grace`() {
        val episode = fullWhileScanning().step(1_000 + grace - 1, running(1, 3))

        assertThat(episode.episodeId).isEqualTo(0L)
        assertFraction(episode.fraction, 1f)
        assertThat(episode.toProgress().processedItems).isEqualTo(10)
        assertThat(episode.fullSinceMillis).isEqualTo(1_000L)
    }

    @Test
    fun `test that invoke starts a new episode at the first sample after the idle grace when files joined a full episode`() {
        val joined = Initial
            .step(0, running(1, 1_000))
            .step(1_000, running(1, 0))
            .step(4_400, running(1, 40))
        val rolledOver = joined.step(16_500, running(1, 39))

        assertThat(joined.episodeId).isEqualTo(0L)
        assertFraction(joined.fraction, 1f)
        assertThat(rolledOver.episodeId).isEqualTo(1L)
        assertThat(rolledOver.toProgress().pendingItems).isEqualTo(39)
        assertThat(rolledOver.fraction).isNull()
    }

    @Test
    fun `test that invoke measures the idle grace from when the episode first became full`() {
        val scanned = (2_100L..11_000L step 1_100).fold(fullWhileScanning()) { episode, millis ->
            episode.step(millis, running(1, 0, scanning = true))
        }
        val episode = scanned.step(12_300, running(1, 5))

        assertThat(episode.episodeId).isEqualTo(1L)
    }

    @Test
    fun `test that invoke keeps a full episode while a sync only scans`() {
        val episode = (10_700L..61_000L step 9_700).fold(fullWhileScanning()) { episode, millis ->
            episode.step(millis, running(1, 0, scanning = true))
        }

        assertThat(episode.episodeId).isEqualTo(0L)
        assertFraction(episode.fraction, 1f)
        assertThat(episode.toProgress().processedItems).isEqualTo(10)
        assertThat(episode.idleSinceMillis).isNull()
    }

    @Test
    fun `test that invoke continues the episode after a resume that holds the fraction below full`() {
        val episodes = listOf(
            0L to running(1, 100),
            1_000L to running(1, 60),
            2_000L to inState(1, SyncRunState.Suspended),
            32_000L to runningWithoutStats(1, since = 32_000),
            32_300L to running(1, 0, scanning = true),
            62_000L to running(1, 0, scanning = true),
            63_000L to running(1, 60),
        ).runningFold(Initial) { episode, (millis, sync) -> episode.step(millis, sync) }.drop(1)
        val resumed = episodes.last()

        episodes.forEach { assertThat(it.fullSinceMillis).isNull() }
        assertThat(resumed.episodeId).isEqualTo(0L)
        assertFraction(resumed.fraction, 0.4f)
    }

    @Test
    fun `test that invoke starts a new episode when a second sync reports files after the first sync's episode became full`() {
        val episode = Initial
            .step(0, running(1, 10), inState(2, SyncRunState.Loading))
            .step(1_000, running(1, 0), inState(2, SyncRunState.Loading))
            .step(8_000, running(1, 0), running(2, 500))

        assertThat(episode.episodeId).isEqualTo(1L)
        assertThat(episode.counters).containsExactly(
            1L, Counter(0, 0, awaitingScanEnd = false),
            2L, Counter(500, 500, awaitingScanEnd = true),
        )
        assertThat(episode.fraction).isNull()
    }

    @Test
    fun `test that invoke gives a new episode after a full one no processed items and no full start`() {
        val episode = deviceRunUntil(71_800).getValue(71_800)

        assertThat(episode.retiredProcessed).isEqualTo(0)
        assertThat(episode.toProgress().processedItems).isEqualTo(0)
        assertThat(episode.fullSinceMillis).isNull()
        assertThat(episode.idleSinceMillis).isNull()
    }

    @Test
    fun `test that invoke records when the episode first became full`() {
        val episodes = steps(
            listOf(running(1, 10)),
            listOf(running(1, 4)),
            listOf(running(1, 0)),
            listOf(running(1, 0)),
        )

        assertThat(episodes.map { it.fullSinceMillis }).containsExactly(null, null, 2_000L, 2_000L)
            .inOrder()
    }

    @Test
    fun `test that invoke starts a new episode for downloads that follow an upload run while the sync keeps scanning`() {
        val episodes = deviceRunUntil(Long.MAX_VALUE)

        fun assertSample(millis: Long, episodeId: Long, fraction: Float?) {
            val episode = episodes.getValue(millis)
            assertThat(episode.episodeId).isEqualTo(episodeId)
            if (fraction == null) assertThat(episode.fraction).isNull()
            else assertThat(episode.fraction!!).isWithin(1e-3f).of(fraction)
        }

        assertSample(1_000, 0, 0f)
        assertSample(3_800, 0, 0.014f)
        assertSample(56_800, 0, 0.998f)
        assertSample(57_800, 0, 0.998f)
        assertSample(58_800, 0, 1f)
        assertSample(71_800, 1, null)
        assertSample(75_000, 1, 0f)
        assertSample(518_000, 1, 0.998f)
        assertSample(519_000, 1, 0.998f)
        assertSample(532_500, 1, 1f)
        assertThat(episodes.getValue(532_500).fullSinceMillis).isEqualTo(532_500L)
        assertThat(episodes.getValue(570_400).idleSinceMillis).isEqualTo(570_400L)
    }
}
