package mega.privacy.android.app.mediaplayer

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayedPastThresholdTrackerTest {

    private lateinit var underTest: PlayedPastThresholdTracker

    @BeforeEach
    fun setUp() {
        underTest = PlayedPastThresholdTracker()
    }

    @Test
    fun `test that isStartOfUnplayedItem returns true for a below threshold snapshot of an item never tracked past it`() {
        val atStart = snapshot(handle = FIRST_ITEM, position = 0L)

        underTest.track(atStart)

        assertThat(underTest.isStartOfUnplayedItem(atStart)).isTrue()
    }

    @Test
    fun `test that isStartOfUnplayedItem returns false for a snapshot past the threshold`() {
        val pastThreshold = snapshot(handle = FIRST_ITEM, position = PAST_THRESHOLD)

        underTest.track(pastThreshold)

        assertThat(underTest.isStartOfUnplayedItem(pastThreshold)).isFalse()
    }

    @Test
    fun `test that isStartOfUnplayedItem returns false when the same item returns below the threshold after being played past it`() {
        underTest.track(snapshot(handle = FIRST_ITEM, position = PAST_THRESHOLD))
        val returnedToStart = snapshot(handle = FIRST_ITEM, position = BELOW_THRESHOLD)

        underTest.track(returnedToStart)

        assertThat(underTest.isStartOfUnplayedItem(returnedToStart)).isFalse()
    }

    @Test
    fun `test that isStartOfUnplayedItem returns true at the start of another item after the previous one was played past the threshold`() {
        underTest.track(snapshot(handle = FIRST_ITEM, position = PAST_THRESHOLD))
        val nextItemStart = snapshot(handle = SECOND_ITEM, position = 0L)

        underTest.track(nextItemStart)

        assertThat(underTest.isStartOfUnplayedItem(nextItemStart)).isTrue()
    }

    @Test
    fun `test that returning to an item played past the threshold earlier starts afresh after another item was tracked`() {
        underTest.track(snapshot(handle = FIRST_ITEM, position = PAST_THRESHOLD))
        underTest.track(snapshot(handle = SECOND_ITEM, position = 0L))
        val backToFirst = snapshot(handle = FIRST_ITEM, position = 0L)

        underTest.track(backToFirst)

        assertThat(underTest.isStartOfUnplayedItem(backToFirst)).isTrue()
    }

    @Test
    fun `test that isStartOfUnplayedItem evaluates the left item against the marker before the next item is tracked`() {
        underTest.track(snapshot(handle = FIRST_ITEM, position = PAST_THRESHOLD))
        val leftAtStart = snapshot(handle = FIRST_ITEM, position = BELOW_THRESHOLD)

        assertThat(underTest.isStartOfUnplayedItem(leftAtStart)).isFalse()
    }

    private fun snapshot(handle: Long, position: Long) = MediaPlaybackInfo(
        mediaHandle = handle,
        totalDuration = 200_000L,
        currentPosition = position,
        mediaType = MediaType.Video,
    )

    private companion object {
        const val FIRST_ITEM = 123456L
        const val SECOND_ITEM = 654321L
        val PAST_THRESHOLD = CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS + 1_000L
        val BELOW_THRESHOLD = CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS - 10_000L
    }
}
