package mega.privacy.android.feature.photos.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlaylistDetailUiEntityMapperTest {
    private lateinit var underTest: VideoPlaylistDetailUiEntityMapper

    private val videoPlaylistUiEntityMapper = mock<VideoPlaylistUiEntityMapper>()
    private val videoUiEntityMapper = mock<VideoUiEntityMapper>()

    private val testPlaylistUiEntity = mock<VideoPlaylistUiEntity>()

    private val videoNodeList = listOf(
        getVideoNode(NodeId(0)),
        getVideoNode(NodeId(1)),
        getVideoNode(NodeId(2)),
        getVideoNode(NodeId(3))
    )

    private val testVideoUiEntities = videoNodeList.map {
        createVideoUiEntity(
            handle = it.id.longValue
        )
    }

    @BeforeAll
    fun setUp() {
        whenever(videoPlaylistUiEntityMapper(any())).thenReturn(testPlaylistUiEntity)
        testVideoUiEntities.onEachIndexed { index, item ->
            whenever(videoUiEntityMapper(videoNodeList[index], emptyList())).thenReturn(item)
        }
        underTest = VideoPlaylistDetailUiEntityMapper(
            videoPlaylistUiEntityMapper = videoPlaylistUiEntityMapper,
            videoUiEntityMapper = videoUiEntityMapper
        )
    }

    @Test
    fun `test that VideoPlaylistUIEntity can be mapped correctly`() = runTest {
        val videoPlaylist = mock<UserVideoPlaylist> {
            on { videos }.thenReturn(videoNodeList)
        }
        val result = underTest(videoPlaylist)
        assertThat(result.uiEntity).isEqualTo(testPlaylistUiEntity)
        assertThat(result.videos).hasSize(videoNodeList.size)
        assertThat(result.videos.map { it.id }).containsAtLeastElementsIn(
            videoNodeList.map { it.id }
        )
    }

    private fun getVideoNode(nodeId: NodeId) = mock<TypedVideoNode> {
        on { id }.thenReturn(nodeId)
        on { thumbnailPath }.thenReturn("thumbnailPath")
        on { hasThumbnail }.thenReturn(true)
    }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        isSharedItems: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = 1L,
        duration = duration,
        isSharedItems = isSharedItems,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", duration),
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
        locations = listOf(LocationFilterOption.AllLocations)
    )
}