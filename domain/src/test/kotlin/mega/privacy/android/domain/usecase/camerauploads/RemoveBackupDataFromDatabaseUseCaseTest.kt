package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

class RemoveBackupDataFromDatabaseUseCaseTest {
    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val underTest: RemoveBackupDataFromDatabaseUseCase =
        RemoveBackupDataFromDatabaseUseCase(cameraUploadsRepository)

    @Test
    fun `test that remove backup data from database use case is called when backup is removed successfully`() = runTest {
        underTest(BackupRemovalStatus(1L, false))
        verify(cameraUploadsRepository).deleteBackupById(backupId = 1L)
    }

    @Test
    fun `test that set backup as outdated is called when backup is not removed successfully`() = runTest {
        underTest(BackupRemovalStatus(1L, true))
        verify(cameraUploadsRepository).setBackupAsOutdated(backupId = 1L)
    }
}