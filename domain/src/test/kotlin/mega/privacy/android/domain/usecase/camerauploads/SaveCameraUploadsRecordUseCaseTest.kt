package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SaveCameraUploadsRecordUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveCameraUploadsRecordUseCaseTest {
    private lateinit var underTest: SaveCameraUploadsRecordUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveCameraUploadsRecordUseCase(cameraUploadsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that the camera uploads records are saved in the database`() = runTest {
        val records = listOf<CameraUploadsRecord>(mock())

        underTest(records)

        verify(cameraUploadsRepository).insertOrUpdateCameraUploadsRecords(records)
    }

}