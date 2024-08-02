package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.repository.LoggingRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
class InitialiseLoggingUseCaseTest {
    private lateinit var underTest: InitialiseLoggingUseCase
    private val sdkMessage = LogEntry(message = "sdk", priority = 1)
    private val chatMessage = LogEntry(message = "chat", priority = 1)

    private val loggingRepository = mock<LoggingRepository> {
        on { getSdkLoggingFlow() }.thenReturn(flowOf(sdkMessage))
        on { getChatLoggingFlow() }.thenReturn(flowOf(chatMessage))
    }

    @Before
    fun setUp() {
        underTest = InitialiseLoggingUseCase(
            loggingRepository = loggingRepository,
            coroutineDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that logs are enabled`() = runTest {

        underTest()

        verify(loggingRepository, times(1)).getSdkLoggingFlow()
        verify(loggingRepository, times(1)).logToSdkFile(sdkMessage)

        verify(loggingRepository, times(1)).getChatLoggingFlow()
        verify(loggingRepository, times(1)).logToChatFile(chatMessage)

        verifyNoMoreInteractions(loggingRepository)
    }
}