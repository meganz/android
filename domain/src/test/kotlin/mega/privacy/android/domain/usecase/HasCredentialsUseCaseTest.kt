package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [HasCredentialsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HasCredentialsUseCaseTest {

    private lateinit var underTest: HasCredentialsUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HasCredentialsUseCase(
            cameraUploadRepository = cameraUploadRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct credentials status is returned`(expected: Boolean) = runTest {
        whenever(cameraUploadRepository.hasCredentials()).thenReturn(expected)
        assertThat(underTest()).isEqualTo(expected)
    }
}
