package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetupMediaUploadsSyncHandleUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupMediaUploadsSyncHandleUseCaseTest {

    private lateinit var underTest: SetupMediaUploadsSyncHandleUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupMediaUploadsSyncHandleUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setupOrUpdateMediaUploadsBackupUseCase = setupOrUpdateMediaUploadsBackupUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository, setupOrUpdateMediaUploadsBackupUseCase)
    }

    @Test
    fun `test that it sets up media sync handle when invoked`() = runTest {
        val handle = 1234L
        underTest(handle)
        verify(cameraUploadRepository).setSecondarySyncHandle(handle)
        verify(setupOrUpdateMediaUploadsBackupUseCase).invoke(
            targetNode = handle,
            localFolder = null
        )
    }
}
