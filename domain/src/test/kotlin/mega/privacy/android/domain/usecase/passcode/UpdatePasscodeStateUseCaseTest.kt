package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.testutils.hotFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class UpdatePasscodeStateUseCaseTest {
    private lateinit var underTest: UpdatePasscodeStateUseCase

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = UpdatePasscodeStateUseCase(
            passcodeRepository = passcodeRepository,
        )
    }

    @Test
    internal fun `test that state is set to locked if passcode timeout is immediate`() = runTest {
        Mockito.clearInvocations(passcodeRepository)

        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
        }

        underTest(34L)

        verify(passcodeRepository).setLocked(true)
    }

    @Test
    internal fun `test that nothing is set if the passcode is not enabled`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(false))
        }

        underTest(12L)

        verify(passcodeRepository, never()).setLocked(any())
    }

    @Test
    internal fun `test that state is set to locked if timeout is longer than elapsed time`() =
        runTest {
            Mockito.clearInvocations(passcodeRepository)
            val timeout = 800L
            val lastLockedTime = 1L
            val resumedTime = lastLockedTime + timeout

            passcodeRepository.stub {
                on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.TimeSpan(timeout)))
                on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
                onBlocking { getLastPausedTime() }.thenReturn(lastLockedTime)
            }

            underTest.invoke(resumedTime)

            verify(passcodeRepository).setLocked(true)
        }

    @Test
    internal fun `test that nothing is set if timeout is shorter than elapsed time`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        val timeout = 800L
        val lastLockedTime = 1L
        val resumedTime = lastLockedTime + timeout - 1

        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.TimeSpan(timeout)))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
            onBlocking { getLastPausedTime() }.thenReturn(lastLockedTime)
        }

        underTest.invoke(resumedTime)

        verify(passcodeRepository, never()).setLocked(any())
    }

    @Test
    internal fun `test that locked state is set to true if last paused time is null`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        val timeout = 800L
        val lastLockedTime = null
        val resumedTime = 1L

        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.TimeSpan(timeout)))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
            onBlocking { getLastPausedTime() }.thenReturn(lastLockedTime)
        }

        underTest.invoke(resumedTime)

        verify(passcodeRepository).setLocked(true)
    }

}
