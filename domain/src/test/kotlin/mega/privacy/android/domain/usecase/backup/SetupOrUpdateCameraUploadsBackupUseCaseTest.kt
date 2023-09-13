package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupOrUpdateCameraUploadsBackupUseCaseTest {

    private lateinit var underTest: SetupOrUpdateCameraUploadsBackupUseCase

    private val getCameraUploadBackupIDUseCase: GetCameraUploadBackupIDUseCase = mock()
    private val setupCameraUploadsBackupUseCase: SetupCameraUploadsBackupUseCase = mock()
    private val updateBackupUseCase: UpdateBackupUseCase = mock()
    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupOrUpdateCameraUploadsBackupUseCase(
            getCameraUploadBackupIDUseCase = getCameraUploadBackupIDUseCase,
            setupCameraUploadsBackupUseCase = setupCameraUploadsBackupUseCase,
            updateBackupUseCase = updateBackupUseCase,
            cameraUploadRepository = cameraUploadRepository,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCameraUploadBackupIDUseCase,
            setupCameraUploadsBackupUseCase,
            updateBackupUseCase,
            cameraUploadRepository,
            isCameraUploadsEnabledUseCase
        )
    }

    @Test
    fun `test that nothing happens when camera uploads is not enabled`() =
        runTest {
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(false)
            underTest(targetNode = 456L, localFolder = "/path/to/local/folder")
            verifyNoInteractions(
                getCameraUploadBackupIDUseCase,
                setupCameraUploadsBackupUseCase,
                updateBackupUseCase,
                cameraUploadRepository,
            )
        }

    @ParameterizedTest(name = "backID is {0}")
    @NullSource
    @ValueSource(longs = [-1L])
    fun `test that camera uploads backup is setup when local back up is not set`(backupId: Long?) =
        runTest {
            val backupName = "Camera Uploads"
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(getCameraUploadBackupIDUseCase()).thenReturn(backupId)
            whenever(cameraUploadRepository.getCameraUploadsName()).thenReturn(backupName)
            underTest(targetNode = 456L, localFolder = "/path/to/local/folder")
            verify(setupCameraUploadsBackupUseCase).invoke(backupName)
        }

    @Test
    fun `test that camera uploads backup is updated when local back up is set`() =
        runTest {
            val backupName = "Camera Uploads"
            val backupId = 1234L
            val targetNode = 456L
            val localFolder = "/path/to/local/folder"
            whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
            whenever(getCameraUploadBackupIDUseCase()).thenReturn(backupId)
            whenever(cameraUploadRepository.getCameraUploadsName()).thenReturn(backupName)
            underTest(targetNode = targetNode, localFolder = localFolder)
            verify(updateBackupUseCase).invoke(
                backupId = backupId,
                backupName = backupName,
                backupType = BackupInfoType.CAMERA_UPLOADS,
                targetNode = targetNode,
                localFolder = localFolder
            )
        }
}
