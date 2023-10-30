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
    internal fun `test that state is set to locked if passcode timeout is immediate and last paused is not null`() =
        runTest {
            Mockito.clearInvocations(passcodeRepository)

            passcodeRepository.stub {
                on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
                on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
                on { monitorLastOrientation() }.thenReturn(hotFlow(null))
                onBlocking { getLastPausedTime() }.thenReturn(33L)
            }

            underTest(34L, 1)

        verify(passcodeRepository).setLocked(true)
    }

    @Test
    internal fun `test that nothing is set if the passcode is not enabled`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(false))
            on { monitorLastOrientation() }.thenReturn(hotFlow(null))
        }

        underTest(12L, 1)

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
                on { monitorLastOrientation() }.thenReturn(hotFlow(null))
            }

            underTest.invoke(resumedTime, 1)

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
            on { monitorLastOrientation() }.thenReturn(hotFlow(null))
        }

        underTest.invoke(resumedTime, 1)

        verify(passcodeRepository, never()).setLocked(any())
    }

    @Test
    internal fun `test that locked state is not set to true if last paused time is null`() =
        runTest {
            Mockito.clearInvocations(passcodeRepository)
            val timeout = 800L
            val lastLockedTime = null
            val resumedTime = 1L

            passcodeRepository.stub {
                on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.TimeSpan(timeout)))
                on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
                onBlocking { getLastPausedTime() }.thenReturn(lastLockedTime)
                on { monitorLastOrientation() }.thenReturn(hotFlow(null))
            }

            underTest.invoke(resumedTime, 1)

            verify(passcodeRepository, never()).setLocked(true)
    }

    @Test
    internal fun `test that lock state is not set to locked on orientation change`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        val orientationChangeGracePeriod = UpdatePasscodeStateUseCase.ROTATION_GRACE_MILLISECONDS
        val lastLockedTime = 1L
        val resumedTime = lastLockedTime + orientationChangeGracePeriod / 2
        val originalOrientation = 2
        val newOrientation = 1

        passcodeRepository.stub {
            on { monitorPasscodeTimeOut() }.thenReturn(hotFlow(PasscodeTimeout.Immediate))
            on { monitorIsPasscodeEnabled() }.thenReturn(hotFlow(true))
            onBlocking { getLastPausedTime() }.thenReturn(lastLockedTime)
            on { monitorLastOrientation() }.thenReturn(hotFlow(originalOrientation))
        }

        underTest.invoke(resumedTime, newOrientation)

        verify(passcodeRepository, never()).setLocked(true)
    }

}
