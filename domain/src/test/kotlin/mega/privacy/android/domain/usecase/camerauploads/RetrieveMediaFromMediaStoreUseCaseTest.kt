package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [RetrieveMediaFromMediaStoreUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetrieveMediaFromMediaStoreUseCaseTest {

    private lateinit var underTest: RetrieveMediaFromMediaStoreUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RetrieveMediaFromMediaStoreUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that invoking the use case returns a combination of list retrieved from the Media Store for each MediaStoreFileType`() =
        runTest {
            val parentPath = ""
            val mediaStoreFileType1 = mock<MediaStoreFileType>()
            val mediaStoreFileType2 = mock<MediaStoreFileType>()
            val types = listOf(mediaStoreFileType1, mediaStoreFileType2)
            val selectionQuery = "selectionQuery"

            val cameraUploadsMediaList1 = listOf<CameraUploadsMedia>(mock(), mock())
            val cameraUploadsMediaList2 = listOf<CameraUploadsMedia>(mock())
            val expected = cameraUploadsMediaList1 + cameraUploadsMediaList2
            whenever(cameraUploadRepository.getMediaSelectionQuery(parentPath))
                .thenReturn(selectionQuery)
            whenever(
                cameraUploadRepository.getMediaList(
                    mediaStoreFileType1,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList1)
            whenever(
                cameraUploadRepository.getMediaList(
                    mediaStoreFileType2,
                    selectionQuery
                )
            ).thenReturn(cameraUploadsMediaList2)
            assertThat(underTest(parentPath, types)).isEqualTo(expected)
        }
}
