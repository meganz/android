package mega.privacy.android.domain.usecase.verification

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetSMSVerificationShownUseCaseTest {

    private val verificationRepository: VerificationRepository = mock()

    private lateinit var underTest: SetSMSVerificationShownUseCase

    @BeforeEach
    fun setUp() {
        underTest = SetSMSVerificationShownUseCase(
            verificationRepository = verificationRepository
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the SMS verification shown status is set correctly`(isShown: Boolean) = runTest {
        underTest(isShown = isShown)

        verify(verificationRepository).setSMSVerificationShown(isShown)
    }

    @AfterEach
    fun tearDown() {
        reset(verificationRepository)
    }
}
