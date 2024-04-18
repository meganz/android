package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsChargingRequiredToUploadContentUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsChargingRequiredToUploadContentUseCaseTest {

    private lateinit var underTest: IsChargingRequiredToUploadContentUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsChargingRequiredToUploadContentUseCase(cameraUploadRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest(name = "is charging required to upload content: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the state is retrieved`(chargingRequired: Boolean) =
        runTest {
            whenever(cameraUploadRepository.isChargingRequiredToUploadContent()).thenReturn(
                chargingRequired
            )

            assertThat(underTest()).isEqualTo(chargingRequired)
        }

    @Test
    fun `test that the state is false if the repository function returns null`() = runTest {
        whenever(cameraUploadRepository.isChargingRequiredToUploadContent()).thenReturn(null)

        assertThat(underTest()).isFalse()
    }
}