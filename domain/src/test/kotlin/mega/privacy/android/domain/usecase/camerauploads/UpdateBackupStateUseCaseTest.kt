package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Test class for [UpdateBackupStateUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateBackupStateUseCaseTest {
    private lateinit var underTest: UpdateBackupStateUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateBackupStateUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that both remote and local backup states are updated`() = runTest {
        val backupId = 123L
        val newBackupState = BackupState.ACTIVE
        val backup = Backup(
            backupId = backupId,
            backupInfoType = BackupInfoType.BACKUP_UPLOAD,
            targetNode = NodeId(123L),
            localFolder = "test/local/folder/path",
            backupName = "Camera Uploads",
            state = BackupState.PAUSE_ALL,
            subState = 1,
            extraData = "",
            targetFolderPath = "",
        )

        cameraUploadsRepository.stub {
            onBlocking {
                updateRemoteBackupState(
                    backupId = backupId,
                    backupState = newBackupState,
                )
            }.thenReturn(newBackupState)
            onBlocking { getBackupById(backupId) }.thenReturn(backup)
        }

        underTest(
            backupId = backupId,
            backupState = newBackupState,
        )
        verify(cameraUploadsRepository).updateLocalBackup(backup.copy(state = newBackupState))
    }

    @Test
    fun `test that only the remote backup state is updated`() = runTest {
        val backupId = 123L
        val newBackupState = BackupState.ACTIVE

        cameraUploadsRepository.stub {
            onBlocking {
                updateRemoteBackupState(
                    backupId = backupId,
                    backupState = newBackupState,
                )
            }.thenReturn(newBackupState)
            onBlocking { getBackupById(backupId) }.thenReturn(null)
        }

        underTest(
            backupId = backupId,
            backupState = newBackupState,
        )
        verify(cameraUploadsRepository, never()).updateLocalBackup(any())
    }
}
