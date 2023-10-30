package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
internal class SetAppPausedTimeUseCaseTest {
    private lateinit var underTest: SetAppPausedTimeUseCase

    private val monitorPasscodeLockStateUseCase = mock<MonitorPasscodeLockStateUseCase>()

    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest =
            SetAppPausedTimeUseCase(
                monitorPasscodeLockStateUseCase = monitorPasscodeLockStateUseCase,
                passcodeRepository = passcodeRepository,
            )
    }

    @Test
    internal fun `test that last paused time is set`() = runTest {
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(flow {
                emit(false)
                awaitCancellation()
            }
            )
        }
        val expected = 123L
        underTest(expected, 1)
        verifyBlocking(passcodeRepository) { setLastPausedTime(expected) }
    }

    @Test
    internal fun `test that timestamp is not updated if the state is currently locked`() = runTest {
        Mockito.clearInvocations(passcodeRepository)
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(flow {
                emit(true)
                awaitCancellation()
            }
            )
        }

        val expected = 123L
        underTest(expected, 1)
        verifyNoInteractions(passcodeRepository)
    }

    @Test
    internal fun `test that orientation is set if state is not locked`() = runTest {
        monitorPasscodeLockStateUseCase.stub {
            on { invoke() }.thenReturn(flow {
                emit(false)
                awaitCancellation()
            }
            )
        }
        val expected = 1
        underTest(123L, expected)
        verifyBlocking(passcodeRepository) { setLastOrientation(expected) }
    }

}