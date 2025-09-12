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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [UpdateSecondaryFolderBackupStateUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateSecondaryFolderBackupStateUseCaseTest {

    private lateinit var underTest: UpdateSecondaryFolderBackupStateUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val updateBackupStateUseCase = mock<UpdateBackupStateUseCase>()

    private val invalidHandle = -1L

    private val fakeBackup = Backup(
        backupId = 123L,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.INVALID,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    private val invalidBackup = Backup(
        backupId = invalidHandle,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "local",
        backupName = "camera uploads",
        state = BackupState.INVALID,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    @BeforeAll
    fun setUp() {
        underTest = UpdateSecondaryFolderBackupStateUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            updateBackupStateUseCase = updateBackupStateUseCase,
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(cameraUploadsRepository, updateBackupStateUseCase)
    }

    @Test
    fun `test that the secondary folder backup state is updated when sync is enabled and backup id exists and is valid and has changed`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)

            underTest(BackupState.ACTIVE)
            verify(updateBackupStateUseCase).invoke(
                backupId = fakeBackup.backupId,
                backupState = BackupState.ACTIVE,
            )
        }

    @Test
    fun `test that the secondary folder backup state is not updated when sync is disabled`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(false)
            underTest(BackupState.ACTIVE)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the secondary folder backup state is not updated when sync is enabled and backup id is invalid`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(false)
            whenever(cameraUploadsRepository.getInvalidHandle()).thenReturn(invalidHandle)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(invalidBackup)

            underTest(backupState = BackupState.ACTIVE)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the secondary folder backup state is not updated when sync is enabled and backup id exists and is valid but not changed`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(fakeBackup)

            underTest(backupState = BackupState.INVALID)
            verifyNoInteractions(updateBackupStateUseCase)
        }

    @Test
    fun `test that the secondary folder backup state is not updated when sync is enabled and backup is null`() =
        runTest {
            whenever(cameraUploadsRepository.isMediaUploadsEnabled()).thenReturn(true)
            whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(null)

            underTest(backupState = BackupState.INVALID)
            verifyNoInteractions(updateBackupStateUseCase)
        }
}
