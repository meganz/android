package mega.privacy.android.app.presentation.videoplayer.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.mediaplayer.queue.model.MediaQueueItemType
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerItem
import mega.privacy.android.icon.pack.R
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerItemMapperTest {
    private lateinit var underTest: VideoPlayerItemMapper

    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()

    private val testNodeHandle: Long = 123456
    private val testNodeName: String = "video player entity"
    private val testType: MediaQueueItemType = MediaQueueItemType.Previous
    private val testSize: Long = 100
    private val testDuration: Duration = 200.seconds
    private val testDurationString = "3:20"

    @BeforeEach
    fun setUp() {
        whenever(durationInSecondsTextMapper(testDuration)).thenReturn(testDurationString)
        underTest = VideoPlayerItemMapper(durationInSecondsTextMapper)
    }

    @Test
    fun `test that VideoPlayerItem can be mapped correctly when thumbnail is null`() =
        runTest {
            val videoPlayerItem = underTest(
                nodeHandle = testNodeHandle,
                nodeName = testNodeName,
                thumbnail = null,
                type = testType,
                size = testSize,
                duration = testDuration
            )
            assertMappedVideoPlayerItem(
                entity = videoPlayerItem,
                expectedThumbnail = null,
            )
        }

    @Test
    fun `test that VideoPlayerItem can be mapped correctly when thumbnail is not null`() =
        runTest {
            val file = mock<File>()
            val videoPlayerItem = underTest(
                nodeHandle = testNodeHandle,
                nodeName = testNodeName,
                thumbnail = file,
                type = testType,
                size = testSize,
                duration = testDuration
            )
            assertMappedVideoPlayerItem(
                entity = videoPlayerItem,
                expectedThumbnail = file,
            )
        }

    private fun assertMappedVideoPlayerItem(
        entity: VideoPlayerItem,
        expectedThumbnail: File?,
    ) {
        entity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlayerItem::class.simpleName}",
                { assertThat(it.icon).isEqualTo(R.drawable.ic_video_medium_solid) },
                { assertThat(it.nodeHandle).isEqualTo(testNodeHandle) },
                { assertThat(it.nodeName).isEqualTo(testNodeName) },
                { assertThat(it.size).isEqualTo(testSize) },
                { assertThat(it.duration).isEqualTo(testDurationString) },
                { assertThat(it.type).isEqualTo(testType) },
                { assertThat(it.thumbnail).isEqualTo(expectedThumbnail) }
            )
        }
    }
}