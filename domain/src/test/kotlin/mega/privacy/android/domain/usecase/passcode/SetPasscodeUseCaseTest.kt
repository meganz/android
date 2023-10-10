package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.SetPasscodeRequest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SetPasscodeUseCaseTest {
    private lateinit var underTest: SetPasscodeUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = SetPasscodeUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that exception is thrown if passcode is empty`() = runTest {
        assertThrows<IllegalArgumentException> {
            underTest(
                SetPasscodeRequest(
                    PasscodeType.Password, ""
                )
            )
        }
    }

    @Test
    internal fun `test that exception is thrown if passcode is blank`() = runTest {
        assertThrows<IllegalArgumentException> {
            underTest(
                SetPasscodeRequest(
                    PasscodeType.Password, "  "
                )
            )
        }
    }

    @Test
    internal fun `test that exception is thrown if pin count does not match`() = runTest {
        val digits = 4
        val passcode = buildString { repeat(digits - 1) { append("1") } }

        assertThrows<IllegalArgumentException> {
            underTest(
                SetPasscodeRequest(
                    PasscodeType.Pin(
                        digits
                    ), passcode
                )
            )
        }
    }

    @Test
    internal fun `test that passcode is set`() = runTest {
        val expected = "password"
        underTest(
            SetPasscodeRequest(
                PasscodeType.Password, expected
            )
        )

        verify(passcodeRepository).setPasscode(expected)
    }

    @Test
    internal fun `test that pin passcode type is set`() = runTest {
        val digits = 7
        val passcode = buildString { repeat(digits) { append("1") } }
        val expected = PasscodeType.Pin(
            digits
        )
        underTest(
            SetPasscodeRequest(
                expected, passcode
            )
        )

        verify(passcodeRepository).setPasscodeType(expected)
    }

    @Test
    internal fun `test that password type is set`() = runTest {
        val expected = PasscodeType.Password
        underTest(
            SetPasscodeRequest(
                expected, "password"
            )
        )

        verify(passcodeRepository).setPasscodeType(expected)
    }

    @Test
    internal fun `test that biometric type is set`() = runTest {
        val expected = PasscodeType.Biometric(PasscodeType.Password)
        underTest(
            SetPasscodeRequest(
                expected, "password"
            )
        )

        verify(passcodeRepository).setPasscodeType(expected)
    }

    @Test
    internal fun `test that exception is thrown if pin count does not match in biometric type`() =
        runTest {
            val digits = 4
            val passcode = buildString { repeat(digits - 1) { append("1") } }
            val innerType = PasscodeType.Pin(
                digits
            )
            val type = PasscodeType.Biometric(innerType)

            assertThrows<IllegalArgumentException> {
                underTest(
                    SetPasscodeRequest(
                        type, passcode
                    )
                )
            }
        }
}