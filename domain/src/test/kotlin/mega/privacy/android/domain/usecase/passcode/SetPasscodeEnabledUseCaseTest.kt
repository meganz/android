package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
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
    internal fun `test that passcode type is cleared if disabled`() = runTest{
        underTest(false)

        verify(passcodeRepository).setPasscodeType(null)
    }
}