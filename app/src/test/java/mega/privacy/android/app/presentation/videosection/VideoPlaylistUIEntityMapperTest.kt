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
    private val testNodeId = NodeId(0)
    private val totalDuration = "10:00"

    private val videoNodeList = listOf(
        getVideoNode(testNodeId),
        getVideoNode(NodeId(1)),
        getVideoNode(NodeId(2)),
        getVideoNode(NodeId(3))
    )

    private fun getVideoNode(nodeId: NodeId) = mock<TypedVideoNode> {
        on { id }.thenReturn(nodeId)
        on { thumbnailPath }.thenReturn("thumbnailPath")
        on { hasThumbnail }.thenReturn(true)
    }

    @BeforeAll
    fun setUp() {
        underTest = VideoPlaylistUIEntityMapper(
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            videoUIEntityMapper = videoUIEntityMapper,
            context = context
        )
    }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is null and video playlist is UserVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            val videoPlaylist = mock<UserVideoPlaylist> {
                on { id }.thenReturn(NodeId(id))
                on { title }.thenReturn(title)
                on { cover }.thenReturn(cover)
                on { creationTime }.thenReturn(creationTime)
                on { modificationTime }.thenReturn(modificationTime)
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = null
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null and video playlist is UserVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())

            val videoPlaylist = mock<UserVideoPlaylist> {
                on { id }.thenReturn(NodeId(id))
                on { title }.thenReturn(title)
                on { cover }.thenReturn(cover)
                on { creationTime }.thenReturn(creationTime)
                on { modificationTime }.thenReturn(modificationTime)
                on { totalDuration }.thenReturn(10.seconds)
                on { videos }.thenReturn(videoNodeList)
            }
            assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = videoNodeList
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is null and video playlist is SystemVideoPlaylist`() =
        runTest {
            val videoPlaylist = mock<SystemVideoPlaylist> {
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = null
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null and video playlist is SystemVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())

            val videoPlaylist = mock<SystemVideoPlaylist> {
                on { videos }.thenReturn(videoNodeList)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = videoNodeList
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is null and video playlist is FavouritesVideoPlaylist`() =
        runTest {
            whenever(context.getString(anyOrNull())).thenReturn("")
            val videoPlaylist = mock<FavouritesVideoPlaylist> {
                on { videos }.thenReturn(null)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = null
            )
        }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly when video is not null and video playlist is FavouritesVideoPlaylist`() =
        runTest {
            whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(totalDuration)
            whenever(videoUIEntityMapper(anyOrNull())).thenReturn(mock())
            whenever(context.getString(anyOrNull())).thenReturn("")

            val videoPlaylist = mock<FavouritesVideoPlaylist> {
                on { videos }.thenReturn(videoNodeList)
            }
            assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
                videoPlaylistUIEntity = underTest(videoPlaylist),
                expectedVideos = videoNodeList
            )
        }

    private fun assertMappedVideoPlaylistUIEntityByUserVideoPlaylist(
        videoPlaylistUIEntity: VideoPlaylistUIEntity,
        expectedVideos: List<TypedVideoNode>?,
    ) {
        videoPlaylistUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlaylistUIEntity::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(id) },
                { assertThat(it.title).isEqualTo(title) },
                { assertThat(it.cover).isEqualTo(cover) },
                { assertThat(it.creationTime).isEqualTo(creationTime) },
                { assertThat(it.modificationTime).isEqualTo(modificationTime) },
                {
                    assertThat(it.thumbnailList?.size).isEqualTo(
                        expectedVideos?.let {
                            if (expectedVideos.size > 4) {
                                4
                            } else {
                                expectedVideos.size
                            }
                        }
                    )
                },
                {
                    assertThat(it.thumbnailList?.get(0)).isEqualTo(expectedVideos?.get(0)?.id)
                },
                { assertThat(it.numberOfVideos).isEqualTo(expectedVideos?.size ?: 0) },
                {
                    assertThat(it.totalDuration).isEqualTo(
                        if (expectedVideos == null)
                            ""
                        else
                            totalDuration
                    )
                },
                { assertThat(it.videos?.size).isEqualTo(expectedVideos?.size) },
                { assertThat(it.isSystemVideoPlayer).isFalse() }
            )
        }
    }

    private fun assertMappedVideoPlaylistUIEntityBySystemVideoPlaylist(
        videoPlaylistUIEntity: VideoPlaylistUIEntity,
        expectedVideos: List<TypedVideoNode>?,
    ) {
        videoPlaylistUIEntity.let {
            Assertions.assertAll(
                "Grouped Assertions of ${VideoPlaylistUIEntity::class.simpleName}",
                { assertThat(it.id.longValue).isEqualTo(-1) },
                {
                    assertThat(it.thumbnailList?.size).isEqualTo(
                        expectedVideos?.let {
                            if (expectedVideos.size > 4) {
                                4
                            } else {
                                expectedVideos.size
                            }
                        }
                    )
                },
                {
                    assertThat(it.thumbnailList?.get(0)).isEqualTo(expectedVideos?.get(0)?.id)
                },
                { assertThat(it.numberOfVideos).isEqualTo(expectedVideos?.size ?: 0) },
                {
                    assertThat(it.totalDuration).isEqualTo(
                        if (expectedVideos == null)
                            ""
                        else
                            totalDuration
                    )
                },
                { assertThat(it.videos?.size).isEqualTo(expectedVideos?.size) },
                { assertThat(it.isSystemVideoPlayer).isTrue() }
            )
        }
    }
}