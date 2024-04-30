package mega.privacy.android.domain.usecase.logging

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoggingRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreChatLogsEnabledUseCaseTest {

    private lateinit var underTest: AreChatLogsEnabledUseCase

    private val repository: LoggingRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = AreChatLogsEnabledUseCase(
            repository = repository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(repository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct boolean value is returned`(value: Boolean) = runTest {
        whenever(repository.isChatLoggingEnabled()) doReturn flowOf(value)

        underTest().test {
            assertThat(expectMostRecentItem()).isEqualTo(value)
        }
    }
}
