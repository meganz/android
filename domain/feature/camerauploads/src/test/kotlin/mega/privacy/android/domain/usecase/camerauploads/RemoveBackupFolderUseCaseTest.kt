package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
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

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val backupId = Random.nextLong()

    @BeforeAll
    fun setUp() {
        underTest = RemoveBackupFolderUseCase(
            cameraUploadsRepository = cameraUploadsRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadsRepository)
    }

    @Test
    fun `test that remove backup folder is not called when the backup id is null`() = runTest {
        whenever(cameraUploadsRepository.getBackupFolderId(any())).thenReturn(null)
        underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
        verify(cameraUploadsRepository, never()).removeBackupFolder(any())
    }


    @Test
    fun `test that error is thrown when removeBackUpFolder throws some error`() = runTest {
        whenever(cameraUploadsRepository.getBackupFolderId(any())).thenReturn(backupId)
        whenever(cameraUploadsRepository.removeBackupFolder(backupId = backupId))
            .thenThrow(RuntimeException("remove backup folder failed"))
        assertThrows<RuntimeException> {
            underTest(cameraUploadFolderType = CameraUploadFolderType.Primary)
        }
    }
}