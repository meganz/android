package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetCameraUploadsRecordGeneratedFingerprintUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetCameraUploadsRecordGeneratedFingerprintUseCaseTest {

    private lateinit var underTest: SetCameraUploadsRecordGeneratedFingerprintUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetCameraUploadsRecordGeneratedFingerprintUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that a fingerprint is generated for the camera uploads record`() = runTest {
        val mediaId = 123456L
        val timestamp = 789012L
        val folderType = CameraUploadFolderType.Primary
        val generatedFingerprint = "fingerprint"

        underTest(
            mediaId = mediaId,
            timestamp = timestamp,
            folderType = folderType,
            generatedFingerprint = generatedFingerprint,
        )

        verify(cameraUploadsRepository).setRecordGeneratedFingerprint(
            mediaId = mediaId,
            timestamp = timestamp,
            folderType = folderType,
            generatedFingerprint = generatedFingerprint,
        )
    }
}