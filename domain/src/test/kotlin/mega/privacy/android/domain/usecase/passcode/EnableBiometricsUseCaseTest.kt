package mega.privacy.android.domain.usecase.passcode

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

class EnableBiometricsUseCaseTest {
    private lateinit var underTest: EnableBiometricsUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = EnableBiometricsUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that current type is wrapped when enabled`() = runTest {
        val current = PasscodeType.Password
        passcodeRepository.stub {
            on { monitorPasscodeType() }.thenReturn(hotFlow(current))
        }

        underTest.invoke()

        verify(passcodeRepository).setPasscodeType(PasscodeType.Biometric(current))
    }


    @Test
    internal fun `test that no change is made if type is already biometric`() =
        runTest {
            val inner = PasscodeType.Password
            passcodeRepository.stub {
                on { monitorPasscodeType() }.thenReturn(hotFlow(PasscodeType.Biometric(inner)))
            }

            underTest.invoke()

            verify(passcodeRepository, never()).setPasscodeType(any())
        }
}