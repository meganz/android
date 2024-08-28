package mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistUIEntityMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistUIEntityMapperTest {
    private lateinit var underTest: VideoPlaylistUIEntityMapper

    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val videoUIEntityMapper = mock<VideoUIEntityMapper>()

    private val id = 123456L
    private val title = "title"
    private val cover: Long? = null
    private val creationTime = 100L
    private val modificationTime = 200L
    private val testNodeId = NodeId(1)
    private val thumbnailList: List<NodeId> = listOf(testNodeId, NodeId(2))
    private val numberOfVideos = 10
    private val totalDuration = "10:00"

    @BeforeAll
    fun setUp() {
        underTest = VideoPlaylistUIEntityMapper(
            durationInSecondsTextMapper,
            videoUIEntityMapper
        )
    }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            val videoPlaylist = mock<VideoPlaylist> {
                on { id }.thenReturn(NodeId(id))
                on { title }.thenReturn(title)
                on { cover }.thenReturn(cover)
                on { creationTime }.thenReturn(creationTime)
                on { modificationTime }.thenReturn(modificationTime)
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntity(
                videoPlaylistUIEntity = underTest(videoPlaylist)
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())

            val testVideos = listOf<TypedVideoNode>(mock(), mock(), mock())

            val videoPlaylist = mock<VideoPlaylist> {
                on { id }.thenReturn(NodeId(id))
                on { title }.thenReturn(title)
                on { cover }.thenReturn(cover)
                on { creationTime }.thenReturn(creationTime)
                on { modificationTime }.thenReturn(modificationTime)
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(testVideos)
            }
            assertMappedVideoPlaylistUIEntity(
                videoPlaylistUIEntity = underTest(videoPlaylist), testVideos.size
            )
        }

    private fun assertMappedVideoPlaylistUIEntity(
        videoPlaylistUIEntity: VideoPlaylistUIEntity,
        expectedVideosSize: Int? = null,
    ) {
        videoPlaylistUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlaylistUIEntity::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(id) },
                { assertThat(it.title).isEqualTo(title) },
                { assertThat(it.cover).isEqualTo(cover) },
                { assertThat(it.creationTime).isEqualTo(creationTime) },
                { assertThat(it.modificationTime).isEqualTo(modificationTime) },
                { assertThat(it.thumbnailList?.size).isEqualTo(thumbnailList.size) },
                { assertThat(it.thumbnailList?.get(0)).isEqualTo(testNodeId) },
                { assertThat(it.numberOfVideos).isEqualTo(numberOfVideos) },
                { assertThat(it.totalDuration).isEqualTo(totalDuration) },
                { assertThat(it.videos?.size).isEqualTo(expectedVideosSize) }
            )
        }
    }
}