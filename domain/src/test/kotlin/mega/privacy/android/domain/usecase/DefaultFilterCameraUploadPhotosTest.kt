package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFilterCameraUploadPhotosTest {
    private lateinit var underTest: FilterCameraUploadPhotos

    private val cameraUploadFolderId = 1L
    private val mediaUploadFolderId = 2L
    private val photosRepository = mock<PhotosRepository> {
        onBlocking { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        onBlocking { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }

    @Before
    fun setUp() {
        underTest = DefaultFilterCameraUploadPhotos(photosRepository = photosRepository)
    }

    @Test
    fun `test that photos in camera upload folder is returned`() = runTest {
        val photo = mock<Photo.Image> {
            on { parentId }.thenReturn(cameraUploadFolderId)
        }
        assertThat(underTest(listOf(photo))).containsExactly(photo)
    }

    @Test
    fun `test that photos in media upload folder are returned`() = runTest {
        val photo = mock<Photo.Image> {
            on { parentId }.thenReturn(mediaUploadFolderId)
        }
        assertThat(underTest(listOf(photo))).containsExactly(photo)
    }

    @Test
    fun `test that photo in neither folder is not returned`() = runTest {
        val photo = mock<Photo.Image> {
            on { parentId }.thenReturn(3L)
        }
        assertThat(underTest(listOf(photo))).isEmpty()
    }

    @Test
    fun `test that non filtered values are returned`() = runTest {
        val mediaUploadPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(mediaUploadFolderId)
        }
        val cameraUploadPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(cameraUploadFolderId)
        }
        val filteredPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(3L)
        }
        assertThat(underTest(listOf(mediaUploadPhoto,
            cameraUploadPhoto,
            filteredPhoto))).containsExactly(mediaUploadPhoto, cameraUploadPhoto)
    }

    @Test
    fun `test that folder ids are fetched only once`() = runTest {
        val mediaUploadPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(mediaUploadFolderId)
        }
        val cameraUploadPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(cameraUploadFolderId)
        }
        val filteredPhoto = mock<Photo.Image> {
            on { parentId }.thenReturn(3L)
        }
        underTest(listOf(mediaUploadPhoto,
            cameraUploadPhoto,
            filteredPhoto))

        verify(photosRepository, times(1)).getCameraUploadFolderId()
        verify(photosRepository, times(1)).getMediaUploadFolderId()
        verifyNoMoreInteractions(photosRepository)
    }

}