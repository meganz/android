package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

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
        underTest(expected, true)
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
        underTest(expected, true)
        verifyNoInteractions(passcodeRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    internal fun `test that configuration change status is set if state is not locked`(
        isConfigurationChanged: Boolean,
    ) = runTest {
        whenever(monitorPasscodeLockStateUseCase()) doReturn flowOf(false)

        underTest(123L, isConfigurationChanged)

        verify(passcodeRepository).setConfigurationChangedStatus(isConfigurationChanged = isConfigurationChanged)
    }
}
