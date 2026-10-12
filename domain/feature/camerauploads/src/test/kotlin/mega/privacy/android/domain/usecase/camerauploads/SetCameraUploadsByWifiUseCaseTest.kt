package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetCameraUploadsByWifiUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetCameraUploadsByWifiUseCaseTest {

    private lateinit var underTest: SetCameraUploadsByWifiUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetCameraUploadsByWifiUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @ParameterizedTest(name = "wifi only: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that camera uploads can now be run on a specific connection type`(wifiOnly: Boolean) =
        runTest {
            underTest(wifiOnly)

            verify(cameraUploadsRepository).setCameraUploadsByWifi(wifiOnly)
        }
}