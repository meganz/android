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
 * Test class for [ClearCameraUploadsRecordUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClearCameraUploadsRecordUseCaseTest {
    private lateinit var underTest: ClearCameraUploadsRecordUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearCameraUploadsRecordUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that primary records are cleared from the database`() = runTest {
        val folderTypes = listOf(CameraUploadFolderType.Primary)

        underTest(folderTypes)

        verify(cameraUploadsRepository).clearRecords(folderTypes)
    }

    @Test
    fun `test that secondary records are cleared from the database`() = runTest {
        val folderTypes = listOf(CameraUploadFolderType.Secondary)

        underTest(folderTypes)

        verify(cameraUploadsRepository).clearRecords(folderTypes)
    }

    @Test
    fun `test that both primary and secondary records are cleared from the database`() = runTest {
        val folderTypes = listOf(CameraUploadFolderType.Primary, CameraUploadFolderType.Secondary)

        underTest(folderTypes)

        verify(cameraUploadsRepository).clearRecords(folderTypes)
    }
}