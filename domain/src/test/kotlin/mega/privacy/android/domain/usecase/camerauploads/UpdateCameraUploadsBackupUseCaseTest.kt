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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [UpdateCameraUploadsBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateCameraUploadsBackupUseCaseTest {

    private lateinit var underTest: UpdateCameraUploadsBackupUseCase

    private val invalidHandle = -1L

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    private val updateBackupUseCase = mock<UpdateBackupUseCase>()

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
        underTest = UpdateCameraUploadsBackupUseCase(
            cameraUploadRepository = cameraUploadRepository,
            updateBackupUseCase = updateBackupUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadRepository, updateBackupUseCase)
    }

    @Test
    internal fun `test that camera uploads status is updated when sync is enabled and backup id exists and is valid and has changed`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(fakeBackup)
            underTest(
                backupName = "", backupState = BackupState.ACTIVE
            )
            verify(updateBackupUseCase).invoke(any(), anyOrNull(), any(), any())
        }

    @Test
    internal fun `test that camera uploads status is not updated when sync is disabled`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(false)
            underTest(
                backupName = "", backupState = BackupState.ACTIVE
            )
            verifyNoInteractions(updateBackupUseCase)
        }

    @Test
    internal fun `test that camera uploads status is not updated when sync is enabled and backup id is invalid`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(invalidBackup)
            underTest(
                backupName = "", backupState = BackupState.ACTIVE
            )
            verifyNoInteractions(updateBackupUseCase)
        }

    @Test
    internal fun `test that camera uploads status is not updated when sync is enabled and backup id exists and is valid but not changed`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(fakeBackup)
            underTest(
                backupName = "", backupState = BackupState.INVALID
            )
            verifyNoInteractions(updateBackupUseCase)
        }

    @Test
    internal fun `test that camera uploads status is not updated when sync is enabled and backup is null`() =
        runTest {
            whenever(cameraUploadRepository.isSyncEnabled()).thenReturn(true)
            whenever(cameraUploadRepository.getCuBackUp()).thenReturn(null)
            underTest(
                backupName = "", backupState = BackupState.INVALID
            )
            verifyNoInteractions(updateBackupUseCase)
        }
}
