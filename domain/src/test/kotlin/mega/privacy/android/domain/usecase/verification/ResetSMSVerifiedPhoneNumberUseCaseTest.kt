package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResetSMSVerifiedPhoneNumberUseCaseTest {

    private val verificationRepository: VerificationRepository = mock()

    private lateinit var underTest: ResetSMSVerifiedPhoneNumberUseCase

    @BeforeEach
    fun setUp() {
        underTest = ResetSMSVerifiedPhoneNumberUseCase(
            verificationRepository = verificationRepository
        )
    }

    @Test
    fun `test that the verified phone number is successfully reset`() = runTest {
        underTest()

        verify(verificationRepository).resetSMSVerifiedPhoneNumber()
    }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
