package mega.privacy.android.data.mapper.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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
    private val videoNode = mock<TypedVideoNode> {
        on { duration }.thenReturn(duration.seconds)
        on { thumbnailPath }.thenReturn("thumbnailPath")
    }
    private val videoNodeList = listOf(
        videoNode, videoNode, videoNode, videoNode
    )
    private val videoNodeWithNullThumbnail = mock<TypedVideoNode> {
        on { duration }.thenReturn(duration.seconds)
        on { thumbnailPath }.thenReturn(null)
    }
    private val videoNodeListWithNullThumbnail = listOf(
        videoNodeWithNullThumbnail, videoNodeWithNullThumbnail, videoNodeWithNullThumbnail
    )

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
                expectedThumbnail = null,
                expectedNumberOfVideos = 0,
                expectedTotalDuration = 0.seconds
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
                expectedThumbnail = videoNodeList[0].thumbnailPath,
                expectedNumberOfVideos = videoNodeList.size,
                expectedTotalDuration = expectedTotalDuration
            )
        }

    @Test
    fun `test that VideoPlaylist can be mapped correctly when thumbnailList item is null`() =
        runTest {
            val expectedTotalDuration = (duration * videoNodeListWithNullThumbnail.size).seconds
            val videoPlaylist = underTest(
                userSet = initUserSet(),
                videoNodeList = videoNodeListWithNullThumbnail,
            )
            assertMappedVideoPlaylistObject(
                videoPlaylist = videoPlaylist,
                expectedThumbnailSize = videoNodeListWithNullThumbnail.size,
                expectedThumbnail = null,
                expectedNumberOfVideos = videoNodeListWithNullThumbnail.size,
                expectedTotalDuration = expectedTotalDuration
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
        expectedThumbnail: String?,
        expectedNumberOfVideos: Int,
        expectedTotalDuration: Duration,
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
                { assertThat(it.thumbnailList?.get(0)).isEqualTo(expectedThumbnail) },
                { assertThat(it.numberOfVideos).isEqualTo(expectedNumberOfVideos) },
                { assertThat(it.totalDuration).isEqualTo(expectedTotalDuration) },
            )
        }
    }
}