package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupRemovalStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.backup.SetupOrUpdateMediaUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupDataFromDatabaseUseCase
import mega.privacy.android.domain.usecase.camerauploads.RemoveBackupFolderUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class RemoveBackupFoldersLogoutTaskTest {
    private lateinit var underTest: RemoveBackupFoldersLogoutTask

    private val removeBackupFolderUseCase = mock<RemoveBackupFolderUseCase>()
    private val removeBackupDataFromDatabaseUseCase = mock<RemoveBackupDataFromDatabaseUseCase>()
    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val setupOrUpdateMediaUploadsBackupUseCase =
        mock<SetupOrUpdateMediaUploadsBackupUseCase>()
    private val setupOrUpdateCameraUploadsBackupUseCase =
        mock<SetupOrUpdateCameraUploadsBackupUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = RemoveBackupFoldersLogoutTask(
            removeBackupFolderUseCase = removeBackupFolderUseCase,
            removeBackupDataFromDatabaseUseCase = removeBackupDataFromDatabaseUseCase,
            cameraUploadsRepository = cameraUploadsRepository,
            setupOrUpdateMediaUploadsBackupUseCase = setupOrUpdateMediaUploadsBackupUseCase,
            setupOrUpdateCameraUploadsBackupUseCase = setupOrUpdateCameraUploadsBackupUseCase
        )
    }

    @Test
    internal fun `test that remove backup folder use case is called with correct parameters`() =
        runTest {
            underTest.onPreLogout()

            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)
        }

    @Test
    internal fun `test that remove backup data from database use case is called when logout is successful`() =
        runTest {
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Primary))
                .thenReturn(BackupRemovalStatus(1L, true))
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Secondary))
                .thenReturn(BackupRemovalStatus(3L, true))
            underTest.onPreLogout()

            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Primary)
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)

            whenever(removeBackupDataFromDatabaseUseCase(BackupRemovalStatus(1L, true)))
                .thenReturn(Unit)
            whenever(removeBackupDataFromDatabaseUseCase(BackupRemovalStatus(3L, true)))
                .thenReturn(Unit)
            underTest.onLogoutSuccess()

            verify(removeBackupDataFromDatabaseUseCase).invoke(BackupRemovalStatus(1L, true))
            verify(removeBackupDataFromDatabaseUseCase).invoke(BackupRemovalStatus(3L, true))
        }

    @Test
    internal fun `test that backups are added when logout failed and backup was removed on pre logout`() =
        runTest {
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Primary))
                .thenReturn(BackupRemovalStatus(1L, false))
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Secondary))
                .thenReturn(BackupRemovalStatus(3L, false))
            underTest.onPreLogout()
            whenever(cameraUploadsRepository.getPrimarySyncHandle()).thenReturn(1L)
            whenever(cameraUploadsRepository.getPrimaryFolderLocalPath()).thenReturn("path")
            whenever(setupOrUpdateCameraUploadsBackupUseCase(1L, "path")).thenReturn(Unit)
            whenever(cameraUploadsRepository.getSecondarySyncHandle()).thenReturn(3L)
            whenever(cameraUploadsRepository.getSecondaryFolderLocalPath()).thenReturn("path2")
            whenever(setupOrUpdateMediaUploadsBackupUseCase(3L, "path2")).thenReturn(Unit)
            underTest.onLogoutFailed(Throwable())

            verify(setupOrUpdateCameraUploadsBackupUseCase).invoke(1L, "path")
            verify(setupOrUpdateMediaUploadsBackupUseCase).invoke(3L, "path2")
        }

    @Test
    internal fun `test that backups are not added when logout failed but it was not removed on pre logout`() =
        runTest {
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Primary))
                .thenReturn(BackupRemovalStatus(1L, true))
            whenever(removeBackupFolderUseCase.invoke(CameraUploadFolderType.Secondary))
                .thenReturn(BackupRemovalStatus(3L, true))
            underTest.onPreLogout()
            underTest.onLogoutFailed(Throwable())

            verifyNoInteractions(setupOrUpdateCameraUploadsBackupUseCase)
            verifyNoInteractions(setupOrUpdateMediaUploadsBackupUseCase)
        }
}