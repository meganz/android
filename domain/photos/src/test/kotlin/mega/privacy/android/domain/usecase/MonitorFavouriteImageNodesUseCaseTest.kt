package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFavouriteImageNodesUseCaseTest {
    private lateinit var underTest: MonitorFavouriteImageNodesUseCase

    private val getAllFavoritesUseCase = mock<GetAllFavoritesUseCase>()
    private val photosRepository = mock<PhotosRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorFavouriteImageNodesUseCase(
            getAllFavoritesUseCase = getAllFavoritesUseCase,
            photosRepository = photosRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getAllFavoritesUseCase, photosRepository)
    }

    @Test
    fun `test that invoke emits image nodes for favourite images and videos in favourites order`() =
        runTest {
            val videoNode = stubFileNode(NodeId(1L), VideoFileTypeInfo("video/mp4", "mp4", 10.seconds))
            val imageNode = stubFileNode(NodeId(2L), StaticImageFileTypeInfo("image/jpeg", "jpg"))
            val videoImageNode = mock<ImageNode>()
            val imageImageNode = mock<ImageNode>()
            whenever(getAllFavoritesUseCase()).thenReturn(flowOf(listOf(videoNode, imageNode)))
            whenever(photosRepository.fetchImageNode(nodeId = NodeId(1L), filterSvg = false))
                .thenReturn(videoImageNode)
            whenever(photosRepository.fetchImageNode(nodeId = NodeId(2L), filterSvg = false))
                .thenReturn(imageImageNode)

            underTest().test {
                assertThat(awaitItem()).containsExactly(videoImageNode, imageImageNode).inOrder()
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke skips favourites that are folders or non media files`() = runTest {
        val folderNode = mock<TypedFolderNode>()
        val pdfNode = stubFileNode(NodeId(3L), PdfFileTypeInfo)
        whenever(getAllFavoritesUseCase()).thenReturn(flowOf(listOf(folderNode, pdfNode)))

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }

        verify(photosRepository, never()).fetchImageNode(any(), any(), any())
    }

    @Test
    fun `test that invoke skips favourites whose image node cannot be fetched`() = runTest {
        val imageNode = stubFileNode(NodeId(4L), StaticImageFileTypeInfo("image/jpeg", "jpg"))
        whenever(getAllFavoritesUseCase()).thenReturn(flowOf(listOf(imageNode)))
        whenever(photosRepository.fetchImageNode(nodeId = NodeId(4L), filterSvg = false))
            .thenReturn(null)

        underTest().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    private fun stubFileNode(nodeId: NodeId, fileType: FileTypeInfo) = mock<TypedFileNode> {
        on { id }.thenReturn(nodeId)
        on { type }.thenReturn(fileType)
    }
}
