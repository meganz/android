package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.GetCurrentCountryCodeUseCase
import mega.privacy.android.domain.usecase.verification.GetFormattedPhoneNumberUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNormalizedPhoneNumberByNetworkUseCaseTest {

    private lateinit var underTest: GetNormalizedPhoneNumberByNetworkUseCase

    private val getFormattedPhoneNumberUseCase: GetFormattedPhoneNumberUseCase = mock()
    private val getCurrentCountryCodeUseCase: GetCurrentCountryCodeUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = GetNormalizedPhoneNumberByNetworkUseCase(
            getFormattedPhoneNumberUseCase = getFormattedPhoneNumberUseCase,
            getCurrentCountryCodeUseCase = getCurrentCountryCodeUseCase
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["+621234567890"])
    @NullSource
    fun `test that normalized phone number is returned when the country code is not null`(expected: String?) =
        runTest {
            val phoneNumber = "1234567890"
            val countryCode = "62"
            whenever(getCurrentCountryCodeUseCase()).thenReturn(countryCode)
            whenever(
                getFormattedPhoneNumberUseCase(
                    phoneNumber = phoneNumber,
                    countryCode = countryCode
                )
            ).thenReturn(expected)

            val actual = underTest(phoneNumber)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that NULL is returned when the country code is null`() = runTest {
        val phoneNumber = "1234567890"
        whenever(getCurrentCountryCodeUseCase()).thenReturn(null)

        val actual = underTest(phoneNumber)

        verifyNoInteractions(getFormattedPhoneNumberUseCase)
        assertThat(actual).isEqualTo(null)
    }

    @AfterEach
    fun tearDown() {
        reset(
            getFormattedPhoneNumberUseCase,
            getCurrentCountryCodeUseCase
        )
    }
}
