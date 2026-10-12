package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.time.Duration.Companion.hours


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAlmostFullStorageBannerVisibilityUseCaseTest {
    private lateinit var underTest: MonitorAlmostFullStorageBannerVisibilityUseCase

    private val repository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorAlmostFullStorageBannerVisibilityUseCase(repository)
    }

    @BeforeEach
    fun resetMock() {
        reset(repository)
    }


    @ParameterizedTest(name = "Test that the almost full storage quota warning banner visibility is {0} when invoked with timestamp: {1}")
    @MethodSource("provideParameters")
    fun `test that the almost full storage quota warning banner visibility is returned when invoked`(
        expected: Boolean,
        timestamp: Long?,
    ) = runTest {
        whenever(repository.monitorAlmostFullStorageBannerClosingTimestamp()).thenReturn(
            flow {
                emit(timestamp)
                awaitCancellation()
            }
        )
        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(false, System.currentTimeMillis()),
        Arguments.of(true, (System.currentTimeMillis() - 24.hours.inWholeMilliseconds)),
        Arguments.of(false, (System.currentTimeMillis() - 20.hours.inWholeMilliseconds)),
        Arguments.of(true, (System.currentTimeMillis() - 25.hours.inWholeMilliseconds)),
        Arguments.of(true, null),
    )
}