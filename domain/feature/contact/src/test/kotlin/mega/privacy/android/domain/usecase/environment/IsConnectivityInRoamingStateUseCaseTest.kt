package mega.privacy.android.domain.usecase.environment

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsConnectivityInRoamingStateUseCaseTest {

    private lateinit var underTest: IsConnectivityInRoamingStateUseCase

    private val verificationRepository: VerificationRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = IsConnectivityInRoamingStateUseCase(verificationRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the use case returns the same roaming value as the repository`(expected: Boolean) =
        runTest {
            whenever(verificationRepository.isRoaming()).thenReturn(expected)

            val actual = underTest()

            assertThat(actual).isEqualTo(expected)
        }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
