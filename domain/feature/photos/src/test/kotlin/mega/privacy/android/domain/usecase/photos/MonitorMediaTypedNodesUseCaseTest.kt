package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorMediaTypedNodesUseCaseTest {

    private lateinit var underTest: MonitorMediaTypedNodesUseCase

    private val photosRepository: PhotosRepository = mock()

    private val mediaNodesFlow = MutableSharedFlow<List<TypedNode>>()

    @BeforeEach
    fun setup() {
        whenever(photosRepository.monitorMediaTypedNodes) doReturn mediaNodesFlow
        underTest = MonitorMediaTypedNodesUseCase(photosRepository = photosRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that the correct media items are returned`() = runTest {
        val nonFileType = mock<TypedNode>()
        val nonMediaFileType = mock<UnknownFileTypeInfo>()
        val nonMediaNode1 = mock<TypedFileNode> {
            on { type } doReturn nonMediaFileType
        }
        val mediaImageType = mock<StaticImageFileTypeInfo>()
        val mediaNode1 = mock<TypedFileNode> {
            on { type } doReturn mediaImageType
        }
        val mediaSvgType = mock<SvgFileTypeInfo>()
        val mediaNode2 = mock<TypedFileNode> {
            on { type } doReturn mediaSvgType
        }
        val mediaVideoType = mock<VideoFileTypeInfo>()
        val mediaNode3 = mock<TypedFileNode> {
            on { type } doReturn mediaVideoType
        }

        underTest().test {
            val nodes = listOf(nonFileType, nonMediaNode1, mediaNode1, mediaNode2, mediaNode3)
            mediaNodesFlow.emit(nodes)

            val expected = listOf(mediaNode1, mediaNode3)
            assertThat(expectMostRecentItem()).isEqualTo(expected)
        }
    }
}
