package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.testutils.hotFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SetPasscodeEnabledUseCaseTest {
    private lateinit var underTest: SetPasscodeEnabledUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = SetPasscodeEnabledUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that passcode is enabled if a pin has been set`() = runTest {
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn("1234")
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
        }

        underTest(true)

        verify(passcodeRepository).setPasscodeEnabled(true)
    }

    @Test
    internal fun `test that no passcode exception is thrown if a pin has not been set`() = runTest {
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn(null)
        }

        assertThrows<NoPasscodeSetException> { underTest(true) }
    }

    @Test
    internal fun `test that passcode is disabled even if no passcode is set`() = runTest {
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn(null)
        }

        underTest(false)

        verify(passcodeRepository).setPasscodeEnabled(false)
    }

    @Test
    internal fun `test that current passcode is cleared if disabled`() = runTest {
        underTest(false)

        verify(passcodeRepository).setPasscode(null)
    }

    @Test
    internal fun `test that passcode type is cleared if disabled`() = runTest {
        underTest(false)

        verify(passcodeRepository).setPasscodeType(null)
    }

    @Test
    fun `test that timeout is defaulted to 30 seconds if not set`() = runTest {
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn("1234")
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(null))
        }

        underTest(true)

        verify(passcodeRepository).setPasscodeTimeOut(PasscodeTimeout.TimeSpan(30 * 1000))
    }

    @Test
    internal fun `test that default timeout is set if passcode is disabled`() = runTest {
        underTest(false)

        verify(passcodeRepository).setPasscodeTimeOut(PasscodeTimeout.TimeSpan(30 * 1000))
    }
}