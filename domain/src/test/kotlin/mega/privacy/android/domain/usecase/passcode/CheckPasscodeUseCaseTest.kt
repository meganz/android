package mega.privacy.android.domain.usecase.passcode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.security.NoPasscodeSetException
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckPasscodeUseCaseTest {
    private lateinit var underTest: CheckPasscodeUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = CheckPasscodeUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that error is thrown if current passcode is null`() = runTest {
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn(null)
        }

        assertThrows<NoPasscodeSetException> { underTest("") }
    }

    @Test
    internal fun `test that true is returned if passcode matches current`() = runTest {
        val passcode = "Matching"
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn(passcode)
        }

        assertThat(underTest(passcode)).isTrue()
    }

    @Test
    internal fun `test that false is returned if passcode does not match current`() = runTest{
        val passcode = "Matching"
        passcodeRepository.stub {
            onBlocking { getPasscode() }.thenReturn(passcode)
        }

        assertThat(underTest(passcode.uppercase())).isFalse()
    }
}