package mega.privacy.android.domain.usecase.logging

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.LoggingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlushLogsUseCaseTest {
    private lateinit var underTest: FlushLogsUseCase

    private val loggingRepository = mock<LoggingRepository>()

    @BeforeEach
    fun setUp() {
        reset(loggingRepository)
        underTest = FlushLogsUseCase(loggingRepository = loggingRepository)
    }

    @Test
    fun `test that invoke flushes the logging repository`() = runTest {
        underTest()

        verify(loggingRepository, times(1)).flush()
        verifyNoMoreInteractions(loggingRepository)
    }
}
