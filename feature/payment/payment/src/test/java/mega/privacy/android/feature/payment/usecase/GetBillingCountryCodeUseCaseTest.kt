package mega.privacy.android.feature.payment.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BillingRepository
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetBillingCountryCodeUseCaseTest {

    private lateinit var underTest: GetBillingCountryCodeUseCase

    private val billingRepository: BillingRepository = mock()
    private val environmentRepository: EnvironmentRepository = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetBillingCountryCodeUseCase(
            environmentRepository = environmentRepository,
            billingRepository = billingRepository
        )
    }

    @Test
    fun `test that use case returns country code from billing repository when available`() =
        runTest {
            val expectedCountryCode = "US"
            whenever(billingRepository.getBillingCountryCode()).thenReturn(expectedCountryCode)

            val actual = underTest()

            assertThat(actual).isEqualTo(expectedCountryCode)
        }

    @Test
    fun `test that use case returns fallback country code when billing repository returns null`() =
        runTest {
            whenever(billingRepository.getBillingCountryCode()).thenReturn(null)
            val locale = Locale("en", "GB")
            whenever(environmentRepository.getLocale()).thenReturn(locale)

            val actual = underTest()

            assertThat(actual).isEqualTo("GB")
        }

    @Test
    fun `test that use case returns empty country code when locale country is empty`() = runTest {
        whenever(billingRepository.getBillingCountryCode()).thenReturn(null)
        val locale = Locale("en", "")
        whenever(environmentRepository.getLocale()).thenReturn(locale)

        val actual = underTest()

        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that use case returns country code from default locale when repository returns null`() =
        runTest {
            whenever(billingRepository.getBillingCountryCode()).thenReturn(null)
            val defaultLocale = Locale("en", "US")
            whenever(environmentRepository.getLocale()).thenReturn(defaultLocale)

            val actual = underTest()

            assertThat(actual).isEqualTo("US")
        }

    @AfterEach
    fun tearDown() {
        reset(billingRepository, environmentRepository)
    }
}
