package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [UpdatePrimaryFolderBackupStateUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdatePrimaryFolderBackupStateUseCaseTest {

    private lateinit var underTest: UpdatePrimaryFolderBackupStateUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val updateBackupStateUseCase = mock<UpdateBackupStateUseCase>()

    private val invalidHandle = -1L

    private val fakeBackup = Backup(
        backupId = 123L,
        backupType = 123,
        targetNode = 123L,
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.INVALID,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    private val invalidBackup = Backup(
        backupId = invalidHandle,
        backupType = 123,
        targetNode = 123L,
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.INVALID,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    @BeforeAll
    fun setUp() {
        underTest = UpdatePrimaryFolderBackupStateUseCase(
            cameraUploadRepository = cameraUploadRepository,
            updateBackupStateUseCase = updateBackupStateUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadRepository, updateBackupStateUseCase)
    }

    @Test
    fun `test that the primary folder backup state is updated when sync is enabled and backup id exists and is valid and has changed`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(fakeBackup)

            underTest(BackupState.ACTIVE)
            verify(updateBackupStateUseCase).invoke(
                backupId = fakeBackup.backupId,
                backupState = BackupState.ACTIVE,
            )
        }

    @Test
    fun `test that the primary folder backup state is not updated when sync is disabled`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(false)
            underTest(backupState = BackupState.ACTIVE)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the primary folder backup state is not updated when sync is enabled and backup id is invalid`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(invalidBackup)

            underTest(backupState = BackupState.ACTIVE)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the primary folder backup state is not updated when sync is enabled and backup id exists and is valid but not changed`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(fakeBackup)

            underTest(backupState = BackupState.INVALID)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the primary folder backup state is not updated when sync is enabled and backup is null`() =
        runTest {
            whenever(cameraUploadRepository.isCameraUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(null)

            underTest(backupState = BackupState.INVALID)
            verifyNoInteractions(updateBackupStateUseCase)
        }
}
