package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class FilterCloudDriveImageNodesUseCaseTest {
    private lateinit var underTest: FilterCloudDriveImageNodesUseCase

    private val cameraUploadFolderId = 1L
    private val mediaUploadFolderId = 2L
    private val photosRepository = mock<PhotosRepository> {
        onBlocking { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        onBlocking { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }

    @Before
    fun setUp() {
        underTest = FilterCloudDriveImageNodesUseCase(photosRepository = photosRepository)
    }

    @Test
    fun `test that imageNodes in camera upload folder are filtered out`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        assertThat(underTest(listOf(imageNode))).isEmpty()
    }

    @Test
    fun `test that imageNodes in media upload folder are filtered out`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        assertThat(underTest(listOf(imageNode))).isEmpty()
    }

    @Test
    fun `test that imageNodes in neither folder are returned`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        assertThat(underTest(listOf(imageNode))).containsExactly(imageNode)
    }

    @Test
    fun `test that non filtered values are returned`() = runTest {
        val mediaUploadImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        val cameraUploadImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        val expectedImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        assertThat(
            underTest(
                listOf(
                    mediaUploadImageNode,
                    cameraUploadImageNode,
                    expectedImageNode
                )
            )
        ).containsExactly(expectedImageNode)
    }

    @Test
    fun `test that folder ids are fetched only once`() = runTest {
        val mediaUploadImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        val cameraUploadImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        val filteredImageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        underTest(
            listOf(
                mediaUploadImageNode,
                cameraUploadImageNode,
                filteredImageNode
            )
        )

        verify(photosRepository, times(1)).getCameraUploadFolderId()
        verify(photosRepository, times(1)).getMediaUploadFolderId()
        verifyNoMoreInteractions(photosRepository)
    }

}