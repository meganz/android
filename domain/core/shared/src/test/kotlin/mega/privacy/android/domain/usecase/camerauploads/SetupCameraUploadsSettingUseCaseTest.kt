package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupCameraUploadsSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupCameraUploadsSettingUseCaseTest {

    private lateinit var underTest: SetupCameraUploadsSettingUseCase

    private val cameraUploadsRepository: CameraUploadsRepository = mock()
    private val updateBackupStateUseCase: UpdateBackupStateUseCase = mock()
    private val setUploadFileNamesKeptUseCase: SetUploadFileNamesKeptUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupCameraUploadsSettingUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            updateBackupStateUseCase = updateBackupStateUseCase,
            setUploadFileNamesKeptUseCase = setUploadFileNamesKeptUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            updateBackupStateUseCase,
            setUploadFileNamesKeptUseCase,
        )
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that camera uploads setting is set when invoked`(isEnabled: Boolean) = runTest {
        val cameraUploadsId = 11111L
        whenever(cameraUploadsRepository.getBackupFolderId(CameraUploadFolderType.Primary)).thenReturn(
            cameraUploadsId
        )
        underTest(isEnabled)
        verify(cameraUploadsRepository).setCameraUploadsEnabled(isEnabled)
        verify(updateBackupStateUseCase).invoke(
            backupId = cameraUploadsId,
            backupState = if (isEnabled) BackupState.ACTIVE else BackupState.DISABLED
        )
    }

    @Test
    fun `test that keep file names is set to true when enabling and camera uploads enabled state is null`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(null)
            underTest(true)
            verify(setUploadFileNamesKeptUseCase).invoke(true)
        }

    @Test
    fun `test that keep file names is not changed when enabling and camera uploads is currently disabled`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest(true)
            verify(setUploadFileNamesKeptUseCase, never()).invoke(any())
        }

    @Test
    fun `test that keep file names is not changed when enabling and camera uploads is currently enabled`() =
        runTest {
            whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(true)
            underTest(true)
            verify(setUploadFileNamesKeptUseCase, never()).invoke(any())
        }

    @Test
    fun `test that keep file names is not changed when disabling camera uploads`() = runTest {
        whenever(cameraUploadsRepository.isCameraUploadsEnabled()).thenReturn(null)
        underTest(false)
        verify(setUploadFileNamesKeptUseCase, never()).invoke(any())
    }
}
