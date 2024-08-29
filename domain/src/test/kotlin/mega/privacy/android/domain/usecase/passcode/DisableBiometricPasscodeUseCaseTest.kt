package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.testutils.hotFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class DisableBiometricPasscodeUseCaseTest {
    private lateinit var underTest: DisableBiometricPasscodeUseCase
    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        reset(
            passcodeRepository
        )
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = DisableBiometricPasscodeUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    fun `test that an exception is thrown if current passcode is not biometric`() = runTest {
        passcodeRepository.stub {
            on { monitorPasscodeType() } doReturn hotFlow(PasscodeType.Password)
        }

        assertThrows<IllegalStateException> { underTest() }
    }

    @Test
    fun `test that fallback is set as new passcode type`() = runTest {
        val expected = PasscodeType.Password
        passcodeRepository.stub {
            on { monitorPasscodeType() } doReturn hotFlow(PasscodeType.Biometric(expected))
        }

        underTest()

        verify(passcodeRepository).setPasscodeType(expected)
    }
}