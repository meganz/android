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

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateBackupStateUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that both remote and local backup states are updated`() = runTest {
        val backupId = 123L
        val newBackupState = BackupState.ACTIVE
        val backup = Backup(
            backupId = backupId,
            backupType = 123,
            targetNode = 123L,
            localFolder = "test/local/folder/path",
            backupName = "Camera Uploads",
            state = BackupState.PAUSE_ALL,
            subState = 1,
            extraData = "",
            targetFolderPath = "",
        )

        cameraUploadRepository.stub {
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
        verify(cameraUploadRepository).updateLocalBackup(backup.copy(state = newBackupState))
    }

    @Test
    fun `test that only the remote backup state is updated`() = runTest {
        val backupId = 123L
        val newBackupState = BackupState.ACTIVE

        cameraUploadRepository.stub {
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
        verify(cameraUploadRepository, never()).updateLocalBackup(any())
    }
}