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
import org.mockito.kotlin.whenever

/**
 * Test class for [SetupMediaUploadsSettingUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupMediaUploadsSettingUseCaseTest {

    private lateinit var underTest: SetupMediaUploadsSettingUseCase

    private val cameraUploadRepository: CameraUploadRepository = mock()
    private val setupMediaUploadsBackupUseCase: SetupMediaUploadsBackupUseCase = mock()
    private val removeBackupFolderUseCase: RemoveBackupFolderUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = SetupMediaUploadsSettingUseCase(
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
    fun `test that media uploads setting is set when invoked`(isEnabled: Boolean) = runTest {
        val mediaUploadsName = "Media Uploads"
        whenever(cameraUploadRepository.getMediaUploadsName()).thenReturn(mediaUploadsName)
        underTest(isEnabled)
        verify(cameraUploadRepository).setSecondaryEnabled(isEnabled)
        if (isEnabled) {
            verify(setupMediaUploadsBackupUseCase).invoke(mediaUploadsName)
            verifyNoInteractions(removeBackupFolderUseCase)
        } else {
            verify(removeBackupFolderUseCase).invoke(CameraUploadFolderType.Secondary)
            verifyNoInteractions(setupMediaUploadsBackupUseCase)
        }
    }
}
