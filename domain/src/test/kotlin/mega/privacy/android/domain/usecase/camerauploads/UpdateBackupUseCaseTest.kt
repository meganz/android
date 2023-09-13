package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.BackupRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [UpdateBackupUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class UpdateBackupUseCaseTest {

    private lateinit var underTest: UpdateBackupUseCase

    private val backupRepository = mock<BackupRepository>()
    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateBackupUseCase(
            backupRepository = backupRepository,
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupRepository, cameraUploadRepository)
    }

    @Test
    fun `test that both remote and local backup names are updated`() = runTest {
        val backupId = 123L
        val newBackupName = "New Backup Name"
        val backup = Backup(
            backupId = backupId,
            backupType = 123,
            targetNode = 123L,
            localFolder = "test/local/folder/path",
            backupName = "Camera Uploads",
            state = BackupState.ACTIVE,
            subState = 1,
            extraData = "",
            targetFolderPath = "",
        )

        whenever(cameraUploadRepository.getBackupById(backupId)).thenReturn(backup)
        underTest(
            backupId = backupId,
            backupName = newBackupName,
            backupType = BackupInfoType.CAMERA_UPLOADS
        )

        verify(backupRepository).updateRemoteBackup(
            backupId = backupId,
            backupName = newBackupName,
            backupType = BackupInfoType.CAMERA_UPLOADS,
            targetNode = -1L,
            localFolder = null,
            state = BackupState.INVALID
        )
        verify(cameraUploadRepository).updateLocalBackup(backup.copy(backupName = newBackupName))
    }

    @Test
    fun `test that only the remote backup name is updated`() = runTest {
        val backupId = 1L
        val newBackupName = "backup"

        whenever(cameraUploadRepository.getBackupById(backupId)).thenReturn(null)
        underTest(
            backupId = backupId,
            backupName = newBackupName,
            backupType = BackupInfoType.CAMERA_UPLOADS
        )

        verify(backupRepository).updateRemoteBackup(
            backupId = backupId,
            backupName = newBackupName,
            backupType = BackupInfoType.CAMERA_UPLOADS,
            targetNode = -1L,
            localFolder = null,
            state = BackupState.INVALID
        )
        verify(cameraUploadRepository, never()).updateLocalBackup(any())
    }
}
