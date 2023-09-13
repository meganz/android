package mega.privacy.android.domain.usecase.backup

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadBackupIDUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupOrUpdateMediaUploadsBackupUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetupOrUpdateMediaUploadsBackupUseCaseTest {

    private lateinit var underTest: SetupOrUpdateMediaUploadsBackupUseCase

    private val getMediaUploadBackupIDUseCase: GetMediaUploadBackupIDUseCase = mock()
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase = mock()
    private val updateBackupUseCase: UpdateBackupUseCase = mock()
    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupOrUpdateMediaUploadsBackupUseCase(
            getMediaUploadBackupIDUseCase,
            setupMediaUploadsBackupUseCase,
            updateBackupUseCase,
            cameraUploadRepository,
            isSecondaryFolderEnabled,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getMediaUploadBackupIDUseCase,
            setupMediaUploadsBackupUseCase,
            updateBackupUseCase,
            cameraUploadRepository,
            isSecondaryFolderEnabled,
        )
    }

    @Test
    fun `test that nothing happens when media uploads is not enabled`() =
        runTest {
            whenever(isSecondaryFolderEnabled()).thenReturn(false)
            underTest(targetNode = 456L, localFolder = "/path/to/local/folder")
            verifyNoInteractions(
                getMediaUploadBackupIDUseCase,
                setupMediaUploadsBackupUseCase,
                updateBackupUseCase,
                cameraUploadRepository,
            )
        }

    @ParameterizedTest(name = "backupID is {0}")
    @NullSource
    @ValueSource(longs = [-1L])
    fun `test that media uploads backup is setup when local back up is not set`(backupId: Long?) =
        runTest {
            val backupName = "Media Uploads"
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getMediaUploadBackupIDUseCase()).thenReturn(backupId)
            whenever(cameraUploadRepository.getMediaUploadsName()).thenReturn(backupName)
            underTest(targetNode = 456L, localFolder = "/path/to/local/folder")
            verify(setupMediaUploadsBackupUseCase).invoke(backupName)
        }

    @Test
    fun `test that media uploads backup is updated when local back up is set`() =
        runTest {
            val backupName = "Media Uploads"
            val backupId = 1234L
            val targetNode = 456L
            val localFolder = "/path/to/local/folder"
            whenever(isSecondaryFolderEnabled()).thenReturn(true)
            whenever(getMediaUploadBackupIDUseCase()).thenReturn(backupId)
            whenever(cameraUploadRepository.getMediaUploadsName()).thenReturn(backupName)
            underTest(targetNode = targetNode, localFolder = localFolder)
            verify(updateBackupUseCase).invoke(
                backupId = backupId,
                backupName = backupName,
                backupType = BackupInfoType.MEDIA_UPLOADS,
                targetNode = targetNode,
                localFolder = localFolder
            )
        }
}
