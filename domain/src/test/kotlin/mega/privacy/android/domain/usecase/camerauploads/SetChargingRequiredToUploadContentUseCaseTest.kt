package mega.privacy.android.domain.usecase.camerauploads

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
 * Test class for [SetChargingRequiredToUploadContentUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetChargingRequiredToUploadContentUseCaseTest {

    private lateinit var underTest: SetChargingRequiredToUploadContentUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetChargingRequiredToUploadContentUseCase(cameraUploadRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "new is charging required when uploading content state: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the new charging required when uploading content state is set`(chargingRequired: Boolean) =
        runTest {
            underTest(chargingRequired)

            verify(cameraUploadRepository).setChargingRequiredToUploadContent(chargingRequired)
        }
}