package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.testutils.hotFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SetBiometricsEnabledUseCaseTest {
    private lateinit var underTest: SetBiometricsEnabledUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = SetBiometricsEnabledUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that current type is wrapped when enabled`() = runTest {
        val current = PasscodeType.Password
        passcodeRepository.stub {
            on { monitorPasscodeType() }.thenReturn(hotFlow(current))
        }

        underTest.invoke(true)

        verify(passcodeRepository).setPasscodeType(PasscodeType.Biometric(current))
    }

    @Test
    internal fun `test that inner type is set as type when disabled`() = runTest {
        val inner = PasscodeType.Password
        passcodeRepository.stub {
            on { monitorPasscodeType() }.thenReturn(hotFlow(PasscodeType.Biometric(inner)))
        }

        underTest.invoke(false)

        verify(passcodeRepository).setPasscodeType(inner)
    }

    @Test
    internal fun `test that no changes are made if disabled but current type is not biometric`() =
        runTest {
            val current = PasscodeType.Password
            passcodeRepository.stub {
                on { monitorPasscodeType() }.thenReturn(hotFlow(current))
            }

            underTest.invoke(false)

            verify(passcodeRepository, never()).setPasscodeType(any())
        }

    @Test
    internal fun `test that no change is made if enabling and type is already biometric`() =
        runTest {
            val inner = PasscodeType.Password
            passcodeRepository.stub {
                on { monitorPasscodeType() }.thenReturn(hotFlow(PasscodeType.Biometric(inner)))
            }

            underTest.invoke(true)

            verify(passcodeRepository, never()).setPasscodeType(any())
        }
}