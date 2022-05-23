package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.LoggingRepository
import mega.privacy.android.app.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.app.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.app.domain.usecase.DefaultInitialiseLogging
import mega.privacy.android.app.domain.usecase.InitialiseLogging
import mega.privacy.android.app.logging.loggers.FileLogMessage
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class DefaultInitialiseLoggingTest {
    private lateinit var underTest: InitialiseLogging
    private val areSdkLogsEnabled = mock<AreSdkLogsEnabled>()
    private val areChatLogsEnabled = mock<AreChatLogsEnabled>()
    private val sdkMessage = FileLogMessage(message = "sdk", priority = 1)
    private val chatMessage = FileLogMessage(message = "chat", priority = 1)

    private val loggingRepository = mock<LoggingRepository>{
        on { getSdkLoggingFlow() }.thenReturn(flowOf(sdkMessage))
        on { getChatLoggingFlow() }.thenReturn(flowOf(chatMessage))
    }

    @Before
    fun setUp() {
        underTest = DefaultInitialiseLogging(
            loggingRepository = loggingRepository,
            areSdkLogsEnabled = areSdkLogsEnabled,
            areChatLogsEnabled = areChatLogsEnabled,
            coroutineDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that debug enables console logs`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(emptyFlow())
        whenever(areChatLogsEnabled()).thenReturn(emptyFlow())
        underTest(true)

        verify(loggingRepository, times(1)).enableLogAllToConsole()
        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting sdk logs setting true, enables sdk logs`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(flowOf(true))
        whenever(areChatLogsEnabled()).thenReturn(emptyFlow())

        underTest(false)

        verify(loggingRepository, times(1)).getSdkLoggingFlow()
        verify(loggingRepository, times(1)).logToSdkFile(sdkMessage)

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting chat logs setting true, enables sdk logs`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(emptyFlow())
        whenever(areChatLogsEnabled()).thenReturn(flowOf(true))

        underTest(false)

        verify(loggingRepository, times(1)).getChatLoggingFlow()
        verify(loggingRepository, times(1)).logToChatFile(chatMessage)

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting sdk logs setting false, disables sdk logs`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(flowOf(false))
        whenever(areChatLogsEnabled()).thenReturn(emptyFlow())

        underTest(false)

        verify(loggingRepository, never()).logToSdkFile(any())

        verifyNoMoreInteractions(loggingRepository)
    }

    @Test
    fun `test that setting chat logs setting false, disables chat logs`() = runTest {
        whenever(areSdkLogsEnabled()).thenReturn(emptyFlow())
        whenever(areChatLogsEnabled()).thenReturn(flowOf(false))

        underTest(false)

        verify(loggingRepository, never()).logToChatFile(any())

        verifyNoMoreInteractions(loggingRepository)
    }
}