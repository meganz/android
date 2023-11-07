package test.mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.VideoNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIVideoMapperTest {
    private lateinit var underTest: UIVideoMapper

    @BeforeAll
    fun setUp() {
        underTest = UIVideoMapper()
    }

    @Test
    fun `test that UIVideo can be mapped correctly`() {
        val expectedId = NodeId(123456L)
        val expectedName = "video file name"
        val expectedSize: Long = 100
        val expectedDuration = 100
        val expectedThumbnail = "video file thumbnail"

        val expectedFileNode = mock<FileNode> {
            on { id }.thenReturn(expectedId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
        }

        val expectedVideoNode = mock<VideoNode> {
            on { fileNode }.thenReturn(expectedFileNode)
            on { duration }.thenReturn(expectedDuration)
            on { thumbnailFilePath }.thenReturn(expectedThumbnail)
        }

        underTest(videoNode = expectedVideoNode).let {
            assertThat(it.id.longValue).isEqualTo(expectedId.longValue)
            assertThat(it.name).isEqualTo(expectedName)
            assertThat(it.size).isEqualTo(expectedSize)
            assertThat(it.duration).isEqualTo(expectedDuration)
            assertThat(it.thumbnail).isEqualTo(expectedThumbnail)
        }
    }
}