package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetupCameraUploadSyncHandleUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupCameraUploadSyncHandleUseCaseTest {

    private lateinit var underTest: SetupCameraUploadSyncHandleUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupCameraUploadSyncHandleUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setupOrUpdateCameraUploadsBackupUseCase = setupOrUpdateCameraUploadsBackupUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository, setupOrUpdateCameraUploadsBackupUseCase)
    }

    @Test
    fun `test that it sets up camera sync handle when invoked`() = runTest {
        val handle = 1234L
        underTest(handle)
        verify(cameraUploadRepository).setPrimarySyncHandle(handle)
        verify(setupOrUpdateCameraUploadsBackupUseCase).invoke(
            targetNode = handle,
            localFolder = null
        )
    }
}
