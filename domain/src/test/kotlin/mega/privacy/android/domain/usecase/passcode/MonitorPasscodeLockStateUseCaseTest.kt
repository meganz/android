package mega.privacy.android.domain.usecase.passcode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
internal class MonitorPasscodeLockStateUseCaseTest {
    private lateinit var underTest: MonitorPasscodeLockStateUseCase
    private val passcodeRepository = mock<PasscodeRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorPasscodeLockStateUseCase(passcodeRepository = passcodeRepository)
    }

    @Test
    internal fun `test that repository values are returned`() = runTest {
        val expected = false
        passcodeRepository.stub { on { monitorLockState() }.thenReturn(flowOf(expected)) }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }
}