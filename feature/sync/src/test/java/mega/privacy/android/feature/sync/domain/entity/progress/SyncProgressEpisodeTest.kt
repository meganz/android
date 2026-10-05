package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Companion.Initial
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Counter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode.Interruption
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncProgressEpisodeTest {

    @Test
    fun `test that toProgress reports the episode totals`() {
        val episode = SyncProgressEpisode(
            episodeId = 0,
            counters = mapOf(1L to Counter(discovered = 10, pending = 4, awaitingScanEnd = false)),
            retiredProcessed = 0,
            interruptions = mapOf(2L to Interruption(pendingItems = 5, sinceMillis = 1_000)),
            fraction = 0.4f,
            idleSinceMillis = null,
        )

        val progress = episode.toProgress()

        assertThat(progress.episodeId).isEqualTo(0L)
        assertThat(progress.processedItems).isEqualTo(6)
        assertThat(progress.pendingItems).isEqualTo(4)
        assertThat(progress.interruptedItems).isEqualTo(5)
        assertThat(progress.fraction).isNotNull()
        assertThat(progress.fraction).isEqualTo(episode.fraction)
    }

    @Test
    fun `test that Initial has no counters and an indeterminate fraction`() {
        assertThat(Initial).isEqualTo(
            SyncProgressEpisode(
                episodeId = 0,
                counters = emptyMap(),
                retiredProcessed = 0,
                interruptions = emptyMap(),
                fraction = null,
                idleSinceMillis = null,
            )
        )
    }

    @Test
    fun `test that None has no items and an indeterminate fraction`() {
        assertThat(SyncEpisodeProgress.None).isEqualTo(
            SyncEpisodeProgress(
                episodeId = 0,
                processedItems = 0,
                pendingItems = 0,
                interruptedItems = 0,
                fraction = null,
            )
        )
    }

    @Test
    fun `test that totalItems is processed plus pending items`() {
        val progress = SyncEpisodeProgress(
            episodeId = 0,
            processedItems = 3,
            pendingItems = 4,
            interruptedItems = 9,
            fraction = null,
        )

        assertThat(progress.totalItems).isEqualTo(7)
    }
}
