package mega.privacy.android.domain.usecase.offline

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import java.util.stream.Stream
import kotlin.test.assertSame


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOfflineWarningMessageVisibilityUseCaseTest {
    private lateinit var underTest: MonitorOfflineWarningMessageVisibilityUseCase

    private val settingsRepository = mock<SettingsRepository>()

    @BeforeAll
    fun setUp() {
        underTest =
            MonitorOfflineWarningMessageVisibilityUseCase(settingsRepository = settingsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository)
    }

    private fun provideMonitorOfflineWarningParameters() = Stream.of(
        Arguments.of(null, true),
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

    @ParameterizedTest(name = "when OfflineWarningMessageVisibility is {0}, emits {1}")
    @MethodSource("provideMonitorOfflineWarningParameters")
    fun `test that the returned values are the correct ones`(
        input: Boolean?,
        expected: Boolean,
    ) =
        runTest {
            settingsRepository.stub {
                on { monitorOfflineWarningMessageVisibility() }.thenReturn(flowOf(input))
            }
            underTest().test {
                assertSame(awaitItem(), expected)
                awaitComplete()
            }
        }
}