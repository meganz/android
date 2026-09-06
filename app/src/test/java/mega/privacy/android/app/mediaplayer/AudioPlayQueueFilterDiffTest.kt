package mega.privacy.android.app.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.mediaplayer.AudioPlayQueueFilterDiff.Action
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AudioPlayQueueFilterDiffTest {

    @Test
    fun `test that resolve returns StopPlayback when the rebuilt queue is empty`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, 2L, 3L),
            oldCurrentIndex = 1,
            newHandles = emptyList(),
        )

        assertThat(action).isEqualTo(Action.StopPlayback)
    }

    @Test
    fun `test that resolve keeps the current item at its new index when it survives the filter`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, 2L, 3L),
            oldCurrentIndex = 1,
            newHandles = listOf(3L, 2L),
        )

        assertThat(action).isEqualTo(Action.KeepCurrent(newIndexOfCurrent = 1))
    }

    @Test
    fun `test that resolve resets onto the nearest later survivor when the current item is filtered out`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, 2L, 3L, 4L),
            oldCurrentIndex = 1,
            newHandles = listOf(4L, 1L),
        )

        // 2L is gone; the next old item still present is 3L (absent) then 4L at new index 0.
        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 0))
    }

    @Test
    fun `test that resolve falls back to the queue start when no later item survived`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, 2L, 3L),
            oldCurrentIndex = 2,
            newHandles = listOf(1L),
        )

        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 0))
    }

    @Test
    fun `test that resolve skips unknown old handles while searching for a survivor`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, null, 3L),
            oldCurrentIndex = 0,
            newHandles = listOf(3L),
        )

        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 0))
    }

    @Test
    fun `test that resolve maps a duplicated survivor handle to its first occurrence`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(1L, 2L),
            oldCurrentIndex = 0,
            newHandles = listOf(9L, 2L, 2L),
        )

        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 1))
    }

    @Test
    fun `test that resolve resets when the current item handle is unknown`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = listOf(null, 2L),
            oldCurrentIndex = 0,
            newHandles = listOf(2L),
        )

        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 0))
    }

    @Test
    fun `test that resolve resets to the queue start when the old timeline is empty`() {
        val action = AudioPlayQueueFilterDiff.resolve(
            oldTimelineHandles = emptyList(),
            oldCurrentIndex = 0,
            newHandles = listOf(1L, 2L),
        )

        assertThat(action).isEqualTo(Action.ResetToSurvivor(startIndex = 0))
    }
}
