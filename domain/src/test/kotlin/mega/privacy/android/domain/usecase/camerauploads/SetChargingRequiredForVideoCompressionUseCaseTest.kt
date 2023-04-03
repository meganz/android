package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

/**
 * Test class for [SetChargingRequiredForVideoCompressionUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetChargingRequiredForVideoCompressionUseCaseTest {

    private lateinit var underTest: SetChargingRequiredForVideoCompressionUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetChargingRequiredForVideoCompressionUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "chargingRequired: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that compressing videos will depend on the device charging status`(chargingRequired: Boolean) =
        runTest {
            underTest(chargingRequired)

            verify(cameraUploadRepository).setChargingRequiredForVideoCompression(chargingRequired)
        }
}