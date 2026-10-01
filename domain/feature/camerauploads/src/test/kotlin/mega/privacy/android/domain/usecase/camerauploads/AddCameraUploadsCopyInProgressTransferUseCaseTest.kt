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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCameraUploadsCopyInProgressTransferUseCaseTest {

    private lateinit var underTest: AddCameraUploadsCopyInProgressTransferUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AddCameraUploadsCopyInProgressTransferUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that invoke calls addCameraUploadsCopyInProgressTransfer in repository`() = runTest {
        val record = mock<CameraUploadsRecord>()

        underTest(record)

        verify(cameraUploadsRepository).addCameraUploadsCopyInProgressTransfer(record)
    }
}
