package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.backup.SetupMediaUploadsBackupUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Test class for [SetupMediaUploadSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupMediaUploadSettingUseCaseTest {

    private lateinit var underTest: SetupMediaUploadSettingUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase = mock()
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupMediaUploadSettingUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setupMediaUploadsBackupUseCase = setupMediaUploadsBackupUseCase,
            removeBackupFolderUseCase = removeBackupFolderUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            setupMediaUploadsBackupUseCase,
            removeBackupFolderUseCase
        )
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that media upload setting is set when invoked`(isEnabled: Boolean) = runTest {
        val mediaUploadName = "Media Uploads"
        underTest(isEnabled, mediaUploadName)
        verify(cameraUploadRepository).setSecondaryEnabled(isEnabled)
        if (isEnabled) {
            verify(setupMediaUploadsBackupUseCase).invoke(mediaUploadName)
            verifyNoInteractions(removeBackupFolderUseCase)
        } else {
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)
            verifyNoInteractions(setupMediaUploadsBackupUseCase)
        }
    }
}
