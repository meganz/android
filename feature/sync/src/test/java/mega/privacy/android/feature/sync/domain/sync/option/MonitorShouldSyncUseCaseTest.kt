package mega.privacy.android.feature.sync.domain.sync.option

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncPauseReasonUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorShouldSyncUseCaseTest {

    private val monitorSyncPauseReasonUseCase: MonitorSyncPauseReasonUseCase = mock()

    private val underTest = MonitorShouldSyncUseCase(
        monitorSyncPauseReasonUseCase = monitorSyncPauseReasonUseCase,
    )

    @AfterEach
    fun resetMocks() {
        reset(monitorSyncPauseReasonUseCase)
    }

    @Test
    fun `test that sync is allowed when there is no pause reason`() = runTest {
        whenever(monitorSyncPauseReasonUseCase()).thenReturn(flowOf(null))

        underTest().test {
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "reason: {0}")
    @EnumSource(SyncPauseReason::class)
    fun `test that sync is not allowed when a pause reason is emitted`(reason: SyncPauseReason) =
        runTest {
            whenever(monitorSyncPauseReasonUseCase()).thenReturn(flowOf(reason))

            underTest().test {
                assertThat(awaitItem()).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sync allowance follows every pause reason change`() = runTest {
        whenever(monitorSyncPauseReasonUseCase()).thenReturn(
            flowOf(null, SyncPauseReason.BatterySaver, null)
        )

        underTest().test {
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }
}
