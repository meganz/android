package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPendingCameraUploadsRecordsUseCaseTest {
    private lateinit var underTest: GetPendingCameraUploadsRecordsUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetPendingCameraUploadsRecordsUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            cameraUploadRepository,
        )
    }

    @Test
    fun `test that the use case is invoked with correct upload status list`() = runTest {
        val expectedTypes = listOf<SyncRecordType>(mock())
        val expectedUploadStatus = listOf(
            CameraUploadsRecordUploadStatus.PENDING,
            CameraUploadsRecordUploadStatus.STARTED,
            CameraUploadsRecordUploadStatus.FAILED
        )
        underTest(expectedTypes)
        verify(cameraUploadRepository)
            .getCameraUploadsRecordByUploadStatusAndTypes(expectedUploadStatus, expectedTypes)
    }
}
