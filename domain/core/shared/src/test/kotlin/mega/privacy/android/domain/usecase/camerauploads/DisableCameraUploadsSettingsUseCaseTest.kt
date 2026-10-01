package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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

    private val setupMediaUploadsSettingUseCase = mock<SetupMediaUploadsSettingUseCase>()
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = DisableCameraUploadsSettingsUseCase(
            setupMediaUploadsSettingUseCase = setupMediaUploadsSettingUseCase,
            setupCameraUploadsSettingUseCase = setupCameraUploadsSettingUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            setupMediaUploadsSettingUseCase,
            setupCameraUploadsSettingUseCase,
        )
    }

    @Test
    fun `test that camera uploads and media uploads settings are updated when the use case is invoked`() =
        runTest {
            underTest()

            verify(setupCameraUploadsSettingUseCase).invoke(false)
            verify(setupMediaUploadsSettingUseCase).invoke(false)
        }
}
