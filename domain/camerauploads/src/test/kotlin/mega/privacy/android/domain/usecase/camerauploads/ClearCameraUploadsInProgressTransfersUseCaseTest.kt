package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearCameraUploadsInProgressTransfersUseCaseTest {

    private lateinit var underTest: ClearCameraUploadsInProgressTransfersUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearCameraUploadsInProgressTransfersUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that invoke calls clearCameraUploadsInProgressTransfers in repository`() = runTest {
        underTest()

        verify(cameraUploadsRepository).clearCameraUploadsInProgressTransfers()
    }
}
