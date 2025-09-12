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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [UpdatePrimaryFolderBackupNameUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class UpdatePrimaryFolderBackupNameUseCaseTest {

    private lateinit var underTest: UpdatePrimaryFolderBackupNameUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val updateBackupUseCase = mock<UpdateBackupUseCase>()

    private val testBackup = Backup(
        backupId = 123L,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "test/local/folder/path",
        backupName = "Camera Uploads",
        state = BackupState.ACTIVE,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    @BeforeAll
    fun setUp() {
        underTest = UpdatePrimaryFolderBackupNameUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            updateBackupUseCase = updateBackupUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(cameraUploadsRepository, isCameraUploadsEnabledUseCase, updateBackupUseCase)

    @Test
    fun `test that the primary folder backup name is updated`() = runTest {
        val testBackupName = "test backup name"

        whenever(isCameraUploadsEnabledUseCase()).thenReturn(true)
        whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(testBackup)

        underTest(testBackupName)

        verify(updateBackupUseCase).invoke(
            backupId = testBackup.backupId,
            backupName = testBackupName,
            backupType = BackupInfoType.CAMERA_UPLOADS
        )
    }

    @ParameterizedTest(name = "when isCameraUploadsEnabled is {0}, backupName is \"{1}\" and isPrimaryFolderBackupSet is {2}")
    @MethodSource("provideErrorParams")
    fun `test that the primary folder backup name is not updated`(
        isCameraUploadsEnabled: Boolean,
        backupName: String,
        isPrimaryFolderBackupSet: Boolean,
    ) = runTest {
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(cameraUploadsRepository.getCuBackUp()).thenReturn(if (isPrimaryFolderBackupSet) testBackup else null)

        underTest(backupName)

        verifyNoInteractions(updateBackupUseCase)
    }

    private fun provideErrorParams() = Stream.of(
        Arguments.of(true, "", true),
        Arguments.of(true, "", false),
        Arguments.of(true, "test backup name", false),
        Arguments.of(false, "", true),
        Arguments.of(false, "", false),
        Arguments.of(false, "test backup name", true),
        Arguments.of(false, "test backup name", false),
    )
}
