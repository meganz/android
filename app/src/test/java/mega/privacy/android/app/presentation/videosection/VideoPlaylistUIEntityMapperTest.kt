package mega.privacy.android.app.presentation.videosection

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistUIEntityMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.FavouritesVideoPlaylist
import mega.privacy.android.domain.entity.videosection.SystemVideoPlaylist
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
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

    private val context = mock<Context>()
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
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            videoUIEntityMapper = videoUIEntityMapper,
            context = context
        )
    }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video playlist is UserVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            val videoPlaylist = mock<UserVideoPlaylist> {
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
            assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = id,
                isSystemVideoPlaylist = false
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null when video playlist is UserVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())

            val testVideos = listOf<TypedVideoNode>(mock(), mock(), mock())

            val videoPlaylist = mock<UserVideoPlaylist> {
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
            assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = id,
                isSystemVideoPlaylist = false,
                expectedVideosSize = testVideos.size
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video playlist is SystemVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            val videoPlaylist = mock<SystemVideoPlaylist> {
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = -1,
                isSystemVideoPlaylist = true
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null when video playlist is SystemVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())

            val testVideos = listOf<TypedVideoNode>(mock(), mock(), mock())

            val videoPlaylist = mock<SystemVideoPlaylist> {
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(testVideos)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = -1,
                isSystemVideoPlaylist = true,
                expectedVideosSize = testVideos.size
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video playlist is FavouritesVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(context.getString(anyOrNull())).thenReturn("")
            val videoPlaylist = mock<FavouritesVideoPlaylist> {
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = -1,
                isSystemVideoPlaylist = true
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null when video playlist is FavouritesVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())
            whenever(context.getString(anyOrNull())).thenReturn("")

            val testVideos = listOf<TypedVideoNode>(mock(), mock(), mock())

            val videoPlaylist = mock<FavouritesVideoPlaylist> {
                on { thumbnailList }.thenReturn(thumbnailList)
                on { numberOfVideos }.thenReturn(numberOfVideos)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(testVideos)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedId = -1,
                isSystemVideoPlaylist = true,
                expectedVideosSize = testVideos.size
            )
        }

    private fun assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
        videoPlaylistUIEntity: VideoPlaylistUIEntity,
        expectedId: Long = id,
        isSystemVideoPlaylist: Boolean,
        expectedVideosSize: Int? = null,
    ) {
        videoPlaylistUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlaylistUIEntity::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(expectedId) },
                { assertThat(it.title).isEqualTo(title) },
                { assertThat(it.cover).isEqualTo(cover) },
                { assertThat(it.creationTime).isEqualTo(creationTime) },
                { assertThat(it.modificationTime).isEqualTo(modificationTime) },
                { assertThat(it.thumbnailList?.size).isEqualTo(thumbnailList.size) },
                { assertThat(it.thumbnailList?.get(0)).isEqualTo(testNodeId) },
                { assertThat(it.numberOfVideos).isEqualTo(numberOfVideos) },
                { assertThat(it.totalDuration).isEqualTo(totalDuration) },
                { assertThat(it.videos?.size).isEqualTo(expectedVideosSize) },
                { assertThat(it.isSystemVideoPlayer).isEqualTo(isSystemVideoPlaylist) }
            )
        }
    }

    private fun assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
        videoPlaylistUIEntity: VideoPlaylistUIEntity,
        expectedId: Long = id,
        isSystemVideoPlaylist: Boolean,
        expectedVideosSize: Int? = null,
    ) {
        videoPlaylistUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlaylistUIEntity::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(expectedId) },
                { assertThat(it.thumbnailList?.size).isEqualTo(thumbnailList.size) },
                { assertThat(it.thumbnailList?.get(0)).isEqualTo(testNodeId) },
                { assertThat(it.numberOfVideos).isEqualTo(numberOfVideos) },
                { assertThat(it.totalDuration).isEqualTo(totalDuration) },
                { assertThat(it.videos?.size).isEqualTo(expectedVideosSize) },
                { assertThat(it.isSystemVideoPlayer).isEqualTo(isSystemVideoPlaylist) }
            )
        }
    }
}