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

class FilterCameraUploadImageNodesUseCaseTest {
    private lateinit var underTest: FilterCameraUploadImageNodesUseCase

    private val cameraUploadFolderId = 1L
    private val mediaUploadFolderId = 2L
    private val photosRepository = mock<PhotosRepository> {
        onBlocking { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        onBlocking { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }

    @Before
    fun setUp() {
        underTest = FilterCameraUploadImageNodesUseCase(photosRepository = photosRepository)
    }

    @Test
    fun `test that imageNodes in camera upload folder is returned`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        assertThat(underTest(listOf(imageNode))).containsExactly(imageNode)
    }

    @Test
    fun `test that imageNodes in media upload folder are returned`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        assertThat(underTest(listOf(imageNode))).containsExactly(imageNode)
    }

    @Test
    fun `test that imageNode in neither folder is not returned`() = runTest {
        val imageNode = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        assertThat(underTest(listOf(imageNode))).isEmpty()
    }

    @Test
    fun `test that non filtered values are returned`() = runTest {
        val mediaUploadPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        val cameraUploadPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        val filteredPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        assertThat(
            underTest(
                listOf(
                    mediaUploadPhoto,
                    cameraUploadPhoto,
                    filteredPhoto
                )
            )
        ).containsExactly(mediaUploadPhoto, cameraUploadPhoto)
    }

    @Test
    fun `test that folder ids are fetched only once`() = runTest {
        val mediaUploadPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(mediaUploadFolderId))
        }
        val cameraUploadPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(cameraUploadFolderId))
        }
        val filteredPhoto = mock<ImageNode> {
            on { parentId }.thenReturn(NodeId(3L))
        }
        underTest(
            listOf(
                mediaUploadPhoto,
                cameraUploadPhoto,
                filteredPhoto
            )
        )

        verify(photosRepository, times(1)).getCameraUploadFolderId()
        verify(photosRepository, times(1)).getMediaUploadFolderId()
        verifyNoMoreInteractions(photosRepository)
    }

}