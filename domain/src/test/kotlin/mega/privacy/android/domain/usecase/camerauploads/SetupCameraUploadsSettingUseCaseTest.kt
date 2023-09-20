package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupCameraUploadsBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupCameraUploadsSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupCameraUploadsSettingUseCaseTest {

    private lateinit var underTest: SetupCameraUploadsSettingUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase = mock()
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupCameraUploadsSettingUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setupCameraUploadsBackupUseCase = setupCameraUploadsBackupUseCase,
            removeBackupFolderUseCase = removeBackupFolderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            setupCameraUploadsBackupUseCase,
            removeBackupFolderUseCase
        )
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that camera uploads setting is set when invoked`(isEnabled: Boolean) = runTest {
        val cameraUploadsName = "Camera Uploads"
        whenever(cameraUploadRepository.getCameraUploadsName()).thenReturn(cameraUploadsName)
        underTest(isEnabled)
        verify(cameraUploadRepository).setCameraUploadsEnabled(isEnabled)
        if (isEnabled) {
            verify(setupCameraUploadsBackupUseCase).invoke(cameraUploadsName)
            verifyNoInteractions(removeBackupFolderUseCase)
        } else {
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
            verifyNoInteractions(setupCameraUploadsBackupUseCase)
        }
    }
}
