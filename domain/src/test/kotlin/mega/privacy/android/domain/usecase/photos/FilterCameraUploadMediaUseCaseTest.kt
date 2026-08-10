package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterCameraUploadMediaUseCaseTest {

    private lateinit var underTest: FilterCameraUploadMediaUseCase

    private val photosRepository = mock<PhotosRepository>()
    private val getCameraUploadsSyncHandlesUseCase = mock<GetCameraUploadsSyncHandlesUseCase>()
    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val cameraUploadFolderId = NodeId(longValue = 1L)
    private val mediaUploadFolderId = NodeId(longValue = 2L)

    @BeforeEach
    fun setUp() = runTest {
        whenever(
            photosRepository.getCameraUploadFolderId()
        ) doReturn cameraUploadFolderId.longValue
        whenever(
            photosRepository.getMediaUploadFolderId()
        ) doReturn mediaUploadFolderId.longValue
        whenever(cameraUploadsRepository.getInvalidHandle()) doReturn -1L
        underTest = FilterCameraUploadMediaUseCase(
            photosRepository = photosRepository,
            getCameraUploadsSyncHandlesUseCase = getCameraUploadsSyncHandlesUseCase,
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository, getCameraUploadsSyncHandlesUseCase, cameraUploadsRepository)
    }

    @Test
    fun `test that photos in camera upload folder is returned`() = runTest {
        val photo = mock<TypedFileNode> {
            on { parentId } doReturn cameraUploadFolderId
        }

        val actual = underTest(listOf(photo))

        assertThat(actual).containsExactly(photo)
    }

    @Test
    fun `test that photos in media upload folder are returned`() = runTest {
        val photo = mock<TypedFileNode> {
            on { parentId } doReturn mediaUploadFolderId
        }

        val actual = underTest(listOf(photo))

        assertThat(actual).containsExactly(photo)
    }

    @Test
    fun `test that photo in neither folder is not returned`() = runTest {
        val photo = mock<TypedFileNode> {
            on { parentId } doReturn NodeId(longValue = 3L)
        }

        val actual = underTest(listOf(photo))

        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that non filtered values are returned`() = runTest {
        val mediaUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn mediaUploadFolderId
        }
        val cameraUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn cameraUploadFolderId
        }
        val filteredPhoto = mock<TypedFileNode> {
            on { parentId } doReturn NodeId(longValue = 3L)
        }

        val actual = underTest(
            listOf(
                mediaUploadPhoto,
                cameraUploadPhoto,
                filteredPhoto
            )
        )

        assertThat(actual).containsExactly(mediaUploadPhoto, cameraUploadPhoto)
    }

    @Test
    fun `test that folder ids are fetched only once`() = runTest {
        val mediaUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn mediaUploadFolderId
        }
        val cameraUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn cameraUploadFolderId
        }
        val filteredPhoto = mock<TypedFileNode> {
            on { parentId } doReturn NodeId(longValue = 3L)
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

    @Test
    fun `test that server resolved handles are used when local folder ids are null`() = runTest {
        whenever(photosRepository.getCameraUploadFolderId()) doReturn null
        whenever(photosRepository.getMediaUploadFolderId()) doReturn null
        whenever(getCameraUploadsSyncHandlesUseCase()) doReturn Pair(
            cameraUploadFolderId.longValue,
            mediaUploadFolderId.longValue
        )
        val cameraUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn cameraUploadFolderId
        }
        val mediaUploadPhoto = mock<TypedFileNode> {
            on { parentId } doReturn mediaUploadFolderId
        }
        val filteredPhoto = mock<TypedFileNode> {
            on { parentId } doReturn NodeId(longValue = 3L)
        }

        val actual = underTest(listOf(cameraUploadPhoto, mediaUploadPhoto, filteredPhoto))

        assertThat(actual).containsExactly(cameraUploadPhoto, mediaUploadPhoto)
    }

    @Test
    fun `test that empty list is returned when local folder ids are null and no camera uploads folder exists on server`() =
        runTest {
            whenever(photosRepository.getCameraUploadFolderId()) doReturn null
            whenever(photosRepository.getMediaUploadFolderId()) doReturn null
            whenever(getCameraUploadsSyncHandlesUseCase()) doReturn null
            val photo = mock<TypedFileNode> {
                on { parentId } doReturn cameraUploadFolderId
            }

            val actual = underTest(listOf(photo))

            assertThat(actual).isEmpty()
        }

    @Test
    fun `test that invalid server handles are ignored when local folder ids are null`() = runTest {
        val invalidHandle = -1L
        whenever(photosRepository.getCameraUploadFolderId()) doReturn null
        whenever(photosRepository.getMediaUploadFolderId()) doReturn null
        whenever(getCameraUploadsSyncHandlesUseCase()) doReturn Pair(invalidHandle, invalidHandle)
        val photo = mock<TypedFileNode> {
            on { parentId } doReturn NodeId(longValue = invalidHandle)
        }

        val actual = underTest(listOf(photo))

        assertThat(actual).isEmpty()
    }
}
