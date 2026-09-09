package mega.privacy.android.domain.usecase.environment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.environment.ScreenLockState
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorScreenLockStateUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorScreenLockStateUseCaseTest {

    private lateinit var underTest: MonitorScreenLockStateUseCase

    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorScreenLockStateUseCase(environmentRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(environmentRepository)
    }

    @Test
    fun `test that the screen lock state is returned from the repository`() = runTest {
        whenever(environmentRepository.monitorScreenLockState()).thenReturn(
            flowOf(ScreenLockState.Locked, ScreenLockState.Unlocked)
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ScreenLockState.Locked)
            assertThat(awaitItem()).isEqualTo(ScreenLockState.Unlocked)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
