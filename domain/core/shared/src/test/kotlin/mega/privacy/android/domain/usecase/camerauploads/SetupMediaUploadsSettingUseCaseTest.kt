package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupMediaUploadsSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupMediaUploadsSettingUseCaseTest {

    private lateinit var underTest: SetupMediaUploadsSettingUseCase

    private val cameraUploadsRepository: CameraUploadsRepository = mock()
    private val updateBackupStateUseCase: UpdateBackupStateUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupMediaUploadsSettingUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            updateBackupStateUseCase = updateBackupStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadsRepository,
            updateBackupStateUseCase,
        )
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that media uploads setting is set when invoked`(isEnabled: Boolean) = runTest {
        val mediaUploadsId = 2222L
        whenever(cameraUploadsRepository.getBackupFolderId(CameraUploadFolderType.Secondary)).thenReturn(
            mediaUploadsId
        )
        underTest(isEnabled)
        verify(cameraUploadsRepository).setSecondaryEnabled(isEnabled)
        verify(updateBackupStateUseCase).invoke(
            backupId = mediaUploadsId,
            backupState = if (isEnabled) BackupState.ACTIVE else BackupState.DISABLED
        )
    }
}
