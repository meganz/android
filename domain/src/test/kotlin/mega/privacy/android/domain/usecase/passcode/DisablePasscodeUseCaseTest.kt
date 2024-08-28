package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class DisablePasscodeUseCaseTest {
    private lateinit var underTest: DisablePasscodeUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = DisablePasscodeUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that passcode is disabled`() = runTest {
        underTest()

        verify(passcodeRepository).setPasscodeEnabled(false)
    }

    @Test
    internal fun `test that current passcode is cleared`() = runTest {
        underTest()

        verify(passcodeRepository).setPasscode(null)
    }

    @Test
    internal fun `test that passcode type is cleared`() = runTest {
        underTest()

        verify(passcodeRepository).setPasscodeType(null)
    }

    @Test
    internal fun `test that timeout is cleared`() = runTest {
        underTest()

        verify(passcodeRepository).setPasscodeTimeOut(null)
    }
}