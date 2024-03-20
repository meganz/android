package mega.privacy.android.domain.usecase.verification

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFormattedPhoneNumberUseCaseTest {

    private lateinit var underTest: GetFormattedPhoneNumberUseCase

    private val verificationRepository: VerificationRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = GetFormattedPhoneNumberUseCase(verificationRepository)
    }

    @ParameterizedTest
    @ValueSource(strings = ["+621234567890"])
    @NullSource
    fun `test that the use case returns the same formatted number value as the repository`(expected: String?) =
        runTest {
            val phoneNumber = "1234567890"
            val countryCode = "62"
            whenever(
                verificationRepository.formatPhoneNumber(
                    number = phoneNumber,
                    countryCode = countryCode
                )
            ).thenReturn(expected)

            val actual = underTest(phoneNumber = phoneNumber, countryCode = countryCode)

            assertThat(actual).isEqualTo(expected)
        }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
