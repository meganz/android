package mega.privacy.android.feature.sync.domain.entity.progress

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SyncProgressStatusTest {

    private val progress = SyncEpisodeProgress(
        episodeId = 0,
        processedItems = 0,
        pendingItems = 4,
        interruptedItems = 9,
        fraction = null,
    )

    @Test
    fun `test that filesLeft is the pending items when transferring`() {
        val status = SyncProgressStatus(
            isSeeded = true,
            phase = SyncProgressPhase.Transferring,
            progress = progress,
        )

        assertThat(status.filesLeft).isEqualTo(4)
    }

    @Test
    fun `test that filesLeft is the interrupted items when paused`() {
        val status = SyncProgressStatus(
            isSeeded = true,
            phase = SyncProgressPhase.Paused(SyncPauseCause.User),
            progress = progress.copy(pendingItems = 0),
        )

        assertThat(status.filesLeft).isEqualTo(9)
    }
}
