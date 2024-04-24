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
class SendSMSVerificationCodeUseCaseTest {

    private val verificationRepository: VerificationRepository = mock()

    private lateinit var underTest: SendSMSVerificationCodeUseCase

    @BeforeEach
    fun setUp() {
        underTest = SendSMSVerificationCodeUseCase(
            verificationRepository = verificationRepository
        )
    }

    @Test
    fun `test that the verification code is sent to the right phone number`() = runTest {
        val phoneNumber = "+419827282822"

        underTest(phoneNumber)

        verify(verificationRepository).sendSMSVerificationCode(phoneNumber)
    }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
