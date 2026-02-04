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
        val result = underTest(
            videoPlaylist = videoPlaylist,
            showHiddenItems = false,
            selectedIds = emptySet()
        )
        assertThat(result.uiEntity).isEqualTo(testPlaylistUiEntity)
        assertThat(result.videos).hasSize(videoNodeList.size)
        assertThat(result.videos.map { it.id }).containsAtLeastElementsIn(
            videoNodeList.map { it.id }
        )
    }

    @Test
    fun `test that when showHiddenItems is false sensitive videos are filtered out`() = runTest {
        val sensitiveNode =
            getVideoNode(NodeId(10), isMarkedSensitive = true, isSensitiveInherited = false)
        val inheritedSensitiveNode =
            getVideoNode(NodeId(11), isMarkedSensitive = false, isSensitiveInherited = true)
        val normalNode =
            getVideoNode(NodeId(12), isMarkedSensitive = false, isSensitiveInherited = false)
        val nodesWithSensitive = listOf(sensitiveNode, inheritedSensitiveNode, normalNode)
        val nonSensitiveUiEntities = listOf(
            createVideoUiEntity(handle = 12L)
        )
        whenever(videoUiEntityMapper(normalNode, emptyList())).thenReturn(nonSensitiveUiEntities[0])
        val videoPlaylist = mock<UserVideoPlaylist> {
            on { videos }.thenReturn(nodesWithSensitive)
        }
        val result = underTest(
            videoPlaylist = videoPlaylist,
            showHiddenItems = false,
            selectedIds = emptySet()
        )
        assertThat(result.videos).hasSize(1)
        assertThat(result.videos[0].id).isEqualTo(NodeId(12))
    }

    @Test
    fun `test that when showHiddenItems is true all videos are included`() = runTest {
        val sensitiveNode =
            getVideoNode(NodeId(20), isMarkedSensitive = true, isSensitiveInherited = false)
        val normalNode =
            getVideoNode(NodeId(21), isMarkedSensitive = false, isSensitiveInherited = false)
        val nodesWithSensitive = listOf(sensitiveNode, normalNode)
        val allUiEntities = listOf(
            createVideoUiEntity(handle = 20L),
            createVideoUiEntity(handle = 21L)
        )
        whenever(videoUiEntityMapper(sensitiveNode, emptyList())).thenReturn(allUiEntities[0])
        whenever(videoUiEntityMapper(normalNode, emptyList())).thenReturn(allUiEntities[1])
        val videoPlaylist = mock<UserVideoPlaylist> {
            on { videos }.thenReturn(nodesWithSensitive)
        }
        val result = underTest(
            videoPlaylist = videoPlaylist,
            showHiddenItems = true,
            selectedIds = emptySet()
        )
        assertThat(result.videos).hasSize(2)
        assertThat(result.videos.map { it.id }).containsExactly(NodeId(20), NodeId(21))
    }

    @Test
    fun `test that videos in selectedIds have isSelected true`() = runTest {
        val videoPlaylist = mock<UserVideoPlaylist> {
            on { videos }.thenReturn(videoNodeList)
        }
        val selectedIds = setOf(1L, 3L)
        val result = underTest(
            videoPlaylist = videoPlaylist,
            showHiddenItems = false,
            selectedIds = selectedIds
        )
        assertThat(result.videos).hasSize(videoNodeList.size)
        result.videos.forEach { video ->
            assertThat(video.isSelected).isEqualTo(video.id.longValue in selectedIds)
        }
    }

    private fun getVideoNode(
        nodeId: NodeId,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = mock<TypedVideoNode> {
        on { id }.thenReturn(nodeId)
        on { thumbnailPath }.thenReturn("thumbnailPath")
        on { hasThumbnail }.thenReturn(true)
        on { this.isMarkedSensitive }.thenReturn(isMarkedSensitive)
        on { this.isSensitiveInherited }.thenReturn(isSensitiveInherited)
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