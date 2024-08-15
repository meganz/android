package mega.privacy.android.data.mapper.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.set.UserSet
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistMapperTest {
    private lateinit var underTest: VideoPlaylistMapper

    private val id = 123456L
    private val name = "title name"
    private val cover: Long? = null
    private val creationTime = 100L
    private val modificationTime = 200L
    private val duration = 100

    private val videoNodeList = listOf(
        getVideoNode(NodeId(0)),
        getVideoNode(NodeId(1)),
        getVideoNode(NodeId(2)),
        getVideoNode(NodeId(3))
    )

    private fun getVideoNode(nodeId: NodeId) = mock<TypedVideoNode> {
        on { id }.thenReturn(nodeId)
        on { duration }.thenReturn(duration.seconds)
        on { thumbnailPath }.thenReturn("thumbnailPath")
        on { hasThumbnail }.thenReturn(true)
    }

    @BeforeAll
    fun setUp() {
        underTest = VideoPlaylistMapper()
    }

    @Test
    fun `test that VideoPlaylist can be mapped correctly when videoNodeList is empty`() =
        runTest {
            val videoPlaylist = underTest(
                userSet = initUserSet(),
                videoNodeList = emptyList(),
            )
            assertMappedVideoPlaylistObject(
                videoPlaylist = videoPlaylist,
                expectedThumbnailSize = null,
                expectedNodeIdRelatedToThumbnail = null,
                expectedNumberOfVideos = 0,
                expectedTotalDuration = 0.seconds,
                expectedVideoSize = 0
            )
        }

    @Test
    fun `test that VideoPlaylist can be mapped correctly when videoNodeList is not empty`() =
        runTest {
            val expectedTotalDuration = (duration * videoNodeList.size).seconds
            val videoPlaylist = underTest(
                userSet = initUserSet(),
                videoNodeList = videoNodeList,
            )
            assertMappedVideoPlaylistObject(
                videoPlaylist = videoPlaylist,
                expectedThumbnailSize = videoNodeList.size,
                expectedNodeIdRelatedToThumbnail = videoNodeList[0].id,
                expectedNumberOfVideos = videoNodeList.size,
                expectedTotalDuration = expectedTotalDuration,
                expectedVideoSize = videoNodeList.size
            )
        }

    @Test
    fun `test that VideoPlaylist can be mapped correctly when thumbnailList item is null`() =
        runTest {
            val videoPlaylist = underTest(
                userSet = initUserSet(),
                videoNodeList = emptyList(),
            )
            assertMappedVideoPlaylistObject(
                videoPlaylist = videoPlaylist,
                expectedThumbnailSize = null,
                expectedNodeIdRelatedToThumbnail = null,
                expectedNumberOfVideos = 0,
                expectedTotalDuration = 0.seconds,
                expectedVideoSize = 0
            )
        }

    private fun initUserSet() = mock<UserSet> {
        on { id }.thenReturn(id)
        on { name }.thenReturn(name)
        on { cover }.thenReturn(cover)
        on { creationTime }.thenReturn(creationTime)
        on { modificationTime }.thenReturn(modificationTime)
    }

    private fun assertMappedVideoPlaylistObject(
        videoPlaylist: VideoPlaylist,
        expectedThumbnailSize: Int?,
        expectedNodeIdRelatedToThumbnail: NodeId?,
        expectedNumberOfVideos: Int,
        expectedTotalDuration: Duration,
        expectedVideoSize: Int?,
    ) {
        videoPlaylist.let {
            assertAll(
                "Grouped Assertions of ${VideoPlaylist::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(id) },
                { assertThat(it.title).isEqualTo(name) },
                { assertThat(it.cover).isEqualTo(cover) },
                { assertThat(it.creationTime).isEqualTo(creationTime) },
                { assertThat(it.modificationTime).isEqualTo(modificationTime) },
                { assertThat(it.thumbnailList?.size).isEqualTo(expectedThumbnailSize) },
                { assertThat(it.thumbnailList?.get(0)).isEqualTo(expectedNodeIdRelatedToThumbnail) },
                { assertThat(it.numberOfVideos).isEqualTo(expectedNumberOfVideos) },
                { assertThat(it.totalDuration).isEqualTo(expectedTotalDuration) },
                { assertThat(it.videos?.size).isEqualTo(expectedVideoSize) }
            )
        }
    }
}