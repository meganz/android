package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DisableCameraUploadsSettingsUseCaseTest {
    private lateinit var underTest: DisableCameraUploadsSettingsUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DisableCameraUploadsSettingsUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
        )
    }

    @Test
    fun `test that camera upload settings are updated when the use case is invoked`() = runTest {
        underTest()

        verify(cameraUploadRepository).setCameraUploadsEnabled(false)
        verify(cameraUploadRepository).setSecondaryEnabled(false)
    }
}
