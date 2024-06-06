package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetCameraUploadsRecordUploadStatusUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetCameraUploadsRecordUploadStatusUseCaseTest {

    private lateinit var underTest: SetCameraUploadsRecordUploadStatusUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetCameraUploadsRecordUploadStatusUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the upload status for a camera uploads record is set`() = runTest {
        val mediaId = 123456L
        val timestamp = 789012L
        val folderType = CameraUploadFolderType.Primary
        val uploadStatus = CameraUploadsRecordUploadStatus.UPLOADED

        underTest(
            mediaId = mediaId,
            timestamp = timestamp,
            folderType = folderType,
            uploadStatus = uploadStatus
        )

        verify(cameraUploadsRepository).setRecordUploadStatus(
            mediaId = mediaId,
            timestamp = timestamp,
            folderType = folderType,
            uploadStatus = uploadStatus,
        )
    }
}