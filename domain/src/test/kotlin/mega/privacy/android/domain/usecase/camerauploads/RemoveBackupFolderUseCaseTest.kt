package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoveBackupFolderUseCaseTest {

    private lateinit var underTest: RemoveBackupFolderUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val backupId = Random.nextLong()

    @BeforeAll
    fun setUp() {
        underTest = RemoveBackupFolderUseCase(
            cameraUploadRepository = cameraUploadRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadRepository)
    }

    @Test
    fun `test that remove backup folder is not called when the backup id is null`() = runTest {
        whenever(cameraUploadRepository.getBackupFolderId(any())).thenReturn(null)
        underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
        verify(cameraUploadRepository, never()).removeBackupFolder(any())
    }

    @Test
    fun `test that the backup folder is deleted from the database when removeBackupFolder return success response`() =
        runTest {
            whenever(cameraUploadRepository.getBackupFolderId(any())).thenReturn(backupId)
            whenever(cameraUploadRepository.removeBackupFolder(backupId))
                .thenReturn(Pair(backupId, 0))
            underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
            verify(cameraUploadRepository).deleteBackupById(backupId)
        }

    @Test
    fun `test that the backup folder is set as outdated when removeBackupFolder did not return success response`() {
        runTest {
            whenever(cameraUploadRepository.getBackupFolderId(any())).thenReturn(backupId)
            whenever(cameraUploadRepository.removeBackupFolder(backupId))
                .thenReturn(Pair(backupId, 120))
            underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
            verify(cameraUploadRepository).setBackupAsOutdated(backupId)
        }
    }

    @Test
    fun `test that error is thrown when removeBackUpFolder throws some error`() = runTest {
        whenever(cameraUploadRepository.getBackupFolderId(any())).thenReturn(backupId)
        whenever(cameraUploadRepository.removeBackupFolder(backupId = backupId))
            .thenThrow(RuntimeException("remove backup folder failed"))
        assertThrows<RuntimeException> {
            underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
        }
    }
}