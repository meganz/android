package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.repository.LoggingRepository
import mega.privacy.android.domain.usecase.logging.AreChatLogsEnabledUseCase
import mega.privacy.android.domain.usecase.logging.AreSdkLogsEnabledUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultInitialiseLoggingTest {
    private lateinit var underTest: InitialiseLogging
    private val areSdkLogsEnabledUseCase = mock<AreSdkLogsEnabledUseCase>()
    private val areChatLogsEnabledUseCase = mock<AreChatLogsEnabledUseCase>()
    private val sdkMessage = LogEntry(message = "sdk", priority = 1)
    private val chatMessage = LogEntry(message = "chat", priority = 1)

    private val loggingRepository = mock<LoggingRepository> {
        on { getSdkLoggingFlow() }.thenReturn(flowOf(sdkMessage))
        on { getChatLoggingFlow() }.thenReturn(flowOf(chatMessage))
    }

    @Before
    fun setUp() {
        underTest = DefaultInitialiseLogging(
            loggingRepository = loggingRepository,
            areSdkLogsEnabledUseCase = areSdkLogsEnabledUseCase,
            areChatLogsEnabledUseCase = areChatLogsEnabledUseCase,
            coroutineDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that setting sdk logs setting true, enables sdk logs`() = runTest {
        whenever(areSdkLogsEnabledUseCase()).thenReturn(flowOf(true))
        whenever(areChatLogsEnabledUseCase()).thenReturn(emptyFlow())

        underTest(false)

        verify(loggingRepository, times(1)).getSdkLoggingFlow()
        verify(loggingRepository, times(1)).logToSdkFile(sdkMessage)

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting chat logs setting true, enables sdk logs`() = runTest {
        whenever(areSdkLogsEnabledUseCase()).thenReturn(emptyFlow())
        whenever(areChatLogsEnabledUseCase()).thenReturn(flowOf(true))

        underTest(false)

        verify(loggingRepository, times(1)).getChatLoggingFlow()
        verify(loggingRepository, times(1)).logToChatFile(chatMessage)

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting sdk logs setting false, disables sdk logs`() = runTest {
        whenever(areSdkLogsEnabledUseCase()).thenReturn(flowOf(false))
        whenever(areChatLogsEnabledUseCase()).thenReturn(emptyFlow())

        underTest(false)

        verify(loggingRepository, never()).logToSdkFile(any())

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting chat logs setting false, disables chat logs`() = runTest {
        whenever(areSdkLogsEnabledUseCase()).thenReturn(emptyFlow())
        whenever(areChatLogsEnabledUseCase()).thenReturn(flowOf(false))

        underTest(false)

        verify(loggingRepository, never()).logToChatFile(any())

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that override supersedes chat and sdk setting`() = runTest{
        whenever(areSdkLogsEnabledUseCase()).thenReturn(flowOf(false))
        whenever(areChatLogsEnabledUseCase()).thenReturn(flowOf(false))

        underTest(true)

        verify(loggingRepository, times(1)).getSdkLoggingFlow()
        verify(loggingRepository, times(1)).logToSdkFile(sdkMessage)
        verify(loggingRepository, times(1)).getChatLoggingFlow()
        verify(loggingRepository, times(1)).logToChatFile(chatMessage)

        verifyNoMoreInteractions(loggingRepository)
    }
}