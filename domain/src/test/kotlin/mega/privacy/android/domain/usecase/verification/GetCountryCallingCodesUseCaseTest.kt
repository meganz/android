package mega.privacy.android.domain.usecase.verification

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCountryCallingCodesUseCaseTest {

    private val verificationRepository: VerificationRepository = mock()

    private lateinit var underTest: GetCountryCallingCodesUseCase

    @BeforeEach
    fun setUp() {
        underTest = GetCountryCallingCodesUseCase(
            verificationRepository = verificationRepository
        )
    }

    @Test
    fun `test that the correct list of country calling codes is returned`() = runTest {
        val listOfCountryCallingCodes = listOf("BD:880,", "AU:61,", "NZ:64,", "IN:91,")
        whenever(
            verificationRepository.getCountryCallingCodes()
        ) doReturn listOfCountryCallingCodes

        val actual = underTest()

        assertThat(actual).isEqualTo(listOfCountryCallingCodes)
    }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
