package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Test class for [InitializeBackupsUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class InitializeBackupsUseCaseTest {

    private lateinit var underTest: InitializeBackupsUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupOrUpdateCameraUploadsBackupUseCase: SetupOrUpdateCameraUploadsBackupUseCase =
        mock()
    private val setupOrUpdateMediaUploadsBackupUseCase: SetupOrUpdateMediaUploadsBackupUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = InitializeBackupsUseCase(
            setupOrUpdateCameraUploadsBackupUseCase = setupOrUpdateCameraUploadsBackupUseCase,
            setupOrUpdateMediaUploadsBackupUseCase = setupOrUpdateMediaUploadsBackupUseCase,
            cameraUploadsRepository = cameraUploadRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setupOrUpdateCameraUploadsBackupUseCase,
            setupOrUpdateMediaUploadsBackupUseCase,
            cameraUploadRepository,
        )
    }

    @Test
    fun `test that camera uploads is setup when invoked`() = runTest {
        val localPath = "/path/to/CU"
        val syncHandle = 1234L
        cameraUploadRepository.stub {
            onBlocking { getPrimaryFolderLocalPath() }.thenReturn(localPath)
            onBlocking { getPrimarySyncHandle() }.thenReturn(syncHandle)
        }
        underTest()
        verify(setupOrUpdateCameraUploadsBackupUseCase).invoke(
            targetNode = syncHandle,
            localFolder = localPath
        )
    }

    @Test
    fun `test that media uploads is setup when invoked`() = runTest {
        val localPath = "/path/to/MU"
        val syncHandle = 1234L
        cameraUploadRepository.stub {
            onBlocking { getSecondaryFolderLocalPath() }.thenReturn(localPath)
            onBlocking { getSecondarySyncHandle() }.thenReturn(syncHandle)
        }
        underTest()
        verify(setupOrUpdateMediaUploadsBackupUseCase).invoke(
            targetNode = syncHandle,
            localFolder = localPath
        )
    }
}
