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
 * Test class for [UpdateSecondaryFolderBackupNameUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExperimentalCoroutinesApi
internal class UpdateSecondaryFolderBackupNameUseCaseTest {

    private lateinit var underTest: UpdateSecondaryFolderBackupNameUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val isMediaUploadsEnabledUseCase = mock<IsMediaUploadsEnabledUseCase>()
    private val updateBackupUseCase = mock<UpdateBackupUseCase>()

    private val testBackup = Backup(
        backupId = 123L,
        backupInfoType = BackupInfoType.BACKUP_UPLOAD,
        targetNode = NodeId(123L),
        localFolder = "test/local/folder/path/secondary",
        backupName = "Media Uploads",
        state = BackupState.ACTIVE,
        subState = 1,
        extraData = "",
        targetFolderPath = "",
    )

    @BeforeAll
    fun setUp() {
        underTest = UpdateSecondaryFolderBackupNameUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            isMediaUploadsEnabledUseCase = isMediaUploadsEnabledUseCase,
            updateBackupUseCase = updateBackupUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(cameraUploadsRepository, isMediaUploadsEnabledUseCase, updateBackupUseCase)

    @Test
    fun `test that the secondary folder backup name is updated`() = runTest {
        val testBackupName = "test secondary backup name"

        whenever(isMediaUploadsEnabledUseCase()).thenReturn(true)
        whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(testBackup)

        underTest(testBackupName)

        verify(updateBackupUseCase).invoke(
            backupId = testBackup.backupId,
            backupName = testBackupName,
            backupType = BackupInfoType.MEDIA_UPLOADS
        )
    }

    @ParameterizedTest(name = "when isSecondaryFolderUploadsEnabled is {0}, backupName is \"{1}\" and isSecondaryFolderBackupSet is {2}")
    @MethodSource("provideErrorParams")
    fun `test that the secondary folder backup name is not updated`(
        isSecondaryFolderUploadsEnabled: Boolean,
        backupName: String,
        isSecondaryFolderBackupSet: Boolean,
    ) = runTest {
        whenever(isMediaUploadsEnabledUseCase()).thenReturn(isSecondaryFolderUploadsEnabled)
        whenever(cameraUploadsRepository.getMuBackUp()).thenReturn(if (isSecondaryFolderBackupSet) testBackup else null)

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
