package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogPriority
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class CreateChatLogEntryTest {
    private lateinit var underTest: CreateLogEntry

    private val traceString = "Trace string"
    private val timeString = "timeString"

    private val createTraceString = mock<CreateTraceString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            traceString
        )
    }

    private val getCurrentTimeString = mock<GetCurrentTimeString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            timeString
        )
    }

    private val appVersion = "v1"

    private val environmentRepository = mock<EnvironmentRepository> {
        onBlocking { getAppInfo() }.thenReturn(AppInfo(appVersion, null))
    }

    @Before
    fun setUp() {
        underTest = CreateChatLogEntry(
            createTraceString = createTraceString,
            getCurrentTimeString = getCurrentTimeString,
            environmentRepository = environmentRepository,
        )
    }

    @Test
    fun `test that logs with non null tags return null`() = runTest {
        val request = CreateLogEntryRequest(
            tag = "Tag",
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)
        assertThat(actual).isNull()
    }

    @Test
    fun `test that logs from sdk loggers has no tag added`() = runTest {
        val loggingClass = "loggingClass"
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = listOf(StackTraceElement(loggingClass, "", "", 1)),
            loggingClasses = emptyList(),
            sdkLoggers = listOf(loggingClass),
        )
        val actual = underTest(request)

        assertThat(actual?.tag).isNull()
    }

    @Test
    fun `test that sdk logs have no trace added`() = runTest {
        val loggingClass = "loggingClass"
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = listOf(StackTraceElement(loggingClass, "", "", 1)),
            loggingClasses = emptyList(),
            sdkLoggers = listOf(loggingClass),
        )
        val actual = underTest(request)

        assertThat(actual?.stackTrace).isNull()
    }

    @Test
    fun `test that trace from non sdk loggers have the client app tag added`() = runTest {
        val clientAppTag = "[clientApp"

        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).contains(clientAppTag)
    }

    @Test
    fun `test that client app logs have the time added to their tag`() = runTest {
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).contains(timeString)
    }

    @Test
    fun `test that client app logs have the correct priority string mapping`() = runTest {
        mapOf(
            LogPriority.VERBOSE to "VERBOSE",
            LogPriority.DEBUG to "DEBUG",
            LogPriority.INFO to "INFO",
            LogPriority.ASSERT to "ASSERT",
            LogPriority.WARN to "WARN",
            LogPriority.ERROR to "ERROR",
            LogPriority.UNKNOWN to "UNKNOWN",
        ).forEach { (priority, expectedString) ->
            val request = CreateLogEntryRequest(
                tag = null,
                message = "message",
                priority = priority,
                throwable = null,
                trace = emptyList(),
                loggingClasses = emptyList(),
                sdkLoggers = emptyList()
            )
            val actual = underTest(request)

            assertThat(actual?.tag).contains(expectedString)
        }
    }

    @Test
    fun `test that client app logs have a stackTrace added`() = runTest {
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.stackTrace).isEqualTo(traceString)
    }

    @Test
    fun `test that client app logs contain the app version`() = runTest {

        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).contains(appVersion)
    }

    @Test
    internal fun `test that app version is only fetched once`() = runTest {
        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val first = underTest(request)
        assertThat(first?.tag).contains(appVersion)

        verify(environmentRepository).getAppInfo()

        val second = underTest(request)
        assertThat(second?.tag).contains(appVersion)

        verifyNoMoreInteractions(environmentRepository)
    }
}