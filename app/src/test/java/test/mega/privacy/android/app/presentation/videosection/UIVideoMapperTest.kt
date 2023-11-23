package test.mega.privacy.android.app.presentation.videosection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIVideoMapperTest {
    private lateinit var underTest: UIVideoMapper

    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val addNodeType = mock<AddNodeType>()

    @BeforeAll
    fun setUp() {
        underTest = UIVideoMapper(
            isAvailableOfflineUseCase,
            addNodeType
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isAvailableOfflineUseCase,
            addNodeType
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that UIVideo can be mapped correctly`() = runTest {
        val expectedId = NodeId(123456L)
        val expectedName = "video file name"
        val expectedSize: Long = 100
        val expectedDurationString = "1:40"
        val expectedThumbnail = "video file thumbnail"

        val expectedFileNode = mock<FileNode> {
            on { id }.thenReturn(expectedId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { isFavourite }.thenReturn(false)
        }

        val expectedVideoNode = mock<VideoNode> {
            on { fileNode }.thenReturn(expectedFileNode)
            on { duration }.thenReturn(100)
            on { thumbnailFilePath }.thenReturn(expectedThumbnail)
        }

        whenever(addNodeType(any())).thenReturn(mock<TypedFileNode>())
        whenever(isAvailableOfflineUseCase(any())).thenReturn(false)

        underTest(videoNode = expectedVideoNode).let {
            assertThat(it.id.longValue).isEqualTo(expectedId.longValue)
            assertThat(it.name).isEqualTo(expectedName)
            assertThat(it.size).isEqualTo(expectedSize)
            assertThat(it.duration).isEqualTo(expectedDurationString)
            assertThat(it.thumbnail?.path).isEqualTo(expectedThumbnail)
        }
    }
}