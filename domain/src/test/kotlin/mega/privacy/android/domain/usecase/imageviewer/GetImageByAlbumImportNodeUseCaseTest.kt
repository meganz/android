package mega.privacy.android.domain.usecase.imageviewer

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedImageNode
import mega.privacy.android.domain.repository.AlbumRepository
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetImageByAlbumImportNodeUseCaseTest {
    private lateinit var underTest: GetImageByAlbumImportNodeUseCase

    private val addImageTypeUseCase = mock<AddImageTypeUseCase>()
    private val getImageUseCase = mock<GetImageUseCase>()
    private val albumRepository = mock<AlbumRepository>()
    private val imageNode = mock<ImageNode>()
    private val typedImageNode = mock<TypedImageNode>()

    private val nodeId = NodeId(1L)
    private val fullSize = false
    private val highPriority = false
    private val resetDownloads: () -> Unit = {}

    @BeforeAll
    fun setUp() {
        underTest = GetImageByAlbumImportNodeUseCase(
            addImageTypeUseCase,
            getImageUseCase,
            albumRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        addImageTypeUseCase,
        getImageUseCase,
        albumRepository,
    )

    @Test
    fun `test that flow of ImageResult is emitted by getImageUseCase`() {
        runTest {
            val expectedThumbnailUri = "thumbMEGA/abc.jpg"
            val expectedPreviewUri = "previewMEGA/abc.jpg"
            val expectedFullSizeUri = "tempMEGA/test.jpg"

            whenever(albumRepository.getPublicPhotoImageNode(nodeId)).thenReturn(imageNode)
            whenever(addImageTypeUseCase.invoke(imageNode)).thenReturn(typedImageNode)
            whenever(getImageUseCase(any(), any(), any(), any())).thenReturn(
                flowOf(
                    ImageResult(
                        thumbnailUri = expectedThumbnailUri,
                        previewUri = expectedPreviewUri,
                        fullSizeUri = expectedFullSizeUri
                    )
                )
            )
            val result =
                underTest.invoke(nodeId.longValue, fullSize, highPriority, resetDownloads)
            result.test {
                val actual = awaitItem()
                assertThat(actual.thumbnailUri).isEqualTo(expectedThumbnailUri)
                assertThat(actual.previewUri).isEqualTo(expectedPreviewUri)
                assertThat(actual.fullSizeUri).isEqualTo(expectedFullSizeUri)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
