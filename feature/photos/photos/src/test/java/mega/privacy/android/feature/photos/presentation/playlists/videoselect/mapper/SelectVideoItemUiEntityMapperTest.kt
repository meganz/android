package mega.privacy.android.feature.photos.presentation.playlists.videoselect.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.shared.nodes.model.NodeSubtitleText
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSubtitleMapper
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectVideoItemUiEntityMapperTest {

    private lateinit var underTest: SelectVideoItemUiEntityMapper

    private val fileTypeIconMapper = mock<FileTypeIconMapper>()
    private val durationInSecondsTextMapper = mock<DurationInSecondsTextMapper>()
    private val nodeSubtitleMapper = mock<NodeSubtitleMapper>()

    private val expectedIconRes = 12345
    private val expectedSubtitle = NodeSubtitleText.Empty
    private val expectedDuration = "1:30"

    @BeforeEach
    fun setUp() {
        underTest = SelectVideoItemUiEntityMapper(
            fileTypeIconMapper = fileTypeIconMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            nodeSubtitleMapper = nodeSubtitleMapper,
        )
        whenever(fileTypeIconMapper(any(), any())).thenReturn(expectedIconRes)
        whenever(nodeSubtitleMapper(any(), any())).thenReturn(expectedSubtitle)
        whenever(durationInSecondsTextMapper(anyOrNull())).thenReturn(expectedDuration)
    }

    @Test
    fun `test that TypedFileNode with video type maps to SelectVideoItemUiEntity correctly`() {
        val nodeId = NodeId(1L)
        val nodeName = "video.mp4"
        val duration = 90.seconds
        val videoType = VideoFileTypeInfo("video", "mp4", duration)
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(nodeId)
            on { name }.thenReturn(nodeName)
            on { type }.thenReturn(videoType)
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(false)
            on { isSensitiveInherited }.thenReturn(false)
            on { isTakenDown }.thenReturn(false)
        }

        val result = underTest(typedFileNode)

        assertThat(result).isEqualTo(
            SelectVideoItemUiEntity(
                id = nodeId,
                name = nodeName,
                title = LocalizedText.Literal(nodeName),
                subtitle = expectedSubtitle,
                iconRes = expectedIconRes,
                isSensitive = false,
                isFolder = false,
                duration = expectedDuration,
                isTakenDown = false,
            )
        )
    }

    @Test
    fun `test that TypedFolderNode maps to SelectVideoItemUiEntity with isFolder true and duration null`() {
        val nodeId = NodeId(2L)
        val nodeName = "Folder"
        val typedFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(nodeId)
            on { name }.thenReturn(nodeName)
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(false)
            on { isSensitiveInherited }.thenReturn(false)
            on { isTakenDown }.thenReturn(false)
        }

        val result = underTest(typedFolderNode)

        assertThat(result.id).isEqualTo(nodeId)
        assertThat(result.name).isEqualTo(nodeName)
        assertThat(result.title).isEqualTo(LocalizedText.Literal(nodeName))
        assertThat(result.isFolder).isTrue()
        assertThat(result.duration).isNull()
        assertThat(result.isSensitive).isFalse()
        assertThat(result.isTakenDown).isFalse()
    }

    @Test
    fun `test that sensitive node maps to isSensitive true`() {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(3L))
            on { name }.thenReturn("sensitive.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(true)
            on { isSensitiveInherited }.thenReturn(false)
            on { isTakenDown }.thenReturn(false)
        }

        val result = underTest(typedFileNode)

        assertThat(result.isSensitive).isTrue()
    }

    @Test
    fun `test that inherited sensitive node maps to isSensitive true`() {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(4L))
            on { name }.thenReturn("inherited.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(false)
            on { isSensitiveInherited }.thenReturn(true)
            on { isTakenDown }.thenReturn(false)
        }

        val result = underTest(typedFileNode)

        assertThat(result.isSensitive).isTrue()
    }

    @Test
    fun `test that taken down node maps to isTakenDown true`() {
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(5L))
            on { name }.thenReturn("taken.mp4")
            on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(false)
            on { isSensitiveInherited }.thenReturn(false)
            on { isTakenDown }.thenReturn(true)
        }

        val result = underTest(typedFileNode)

        assertThat(result.isTakenDown).isTrue()
    }

    @Test
    fun `test that file node with non-video type maps to duration null`() {
        whenever(durationInSecondsTextMapper(null)).thenReturn(null)
        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(6L))
            on { name }.thenReturn("file.txt")
            on { type }.thenReturn(TextFileTypeInfo("text", "txt"))
            on { isNodeKeyDecrypted }.thenReturn(true)
            on { isMarkedSensitive }.thenReturn(false)
            on { isSensitiveInherited }.thenReturn(false)
            on { isTakenDown }.thenReturn(false)
        }

        val result = underTest(typedFileNode)

        assertThat(result.duration).isNull()
    }
}
