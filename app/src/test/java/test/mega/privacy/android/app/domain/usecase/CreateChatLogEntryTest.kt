package test.mega.privacy.android.app.domain.usecase

import android.util.Log
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.app.domain.usecase.CreateChatLogEntry
import mega.privacy.android.app.domain.usecase.CreateLogEntry
import mega.privacy.android.app.domain.usecase.CreateTraceString
import mega.privacy.android.app.domain.usecase.GetCurrentTimeString
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class CreateChatLogEntryTest {
    private lateinit var underTest: CreateLogEntry

    private val traceString = "Trace string"
    private val timeString = "timeString"

    private val createTraceString = mock<CreateTraceString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            traceString)
    }

    private val getCurrentTimeString = mock<GetCurrentTimeString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            timeString)
    }

    @Before
    fun setUp() {
        underTest = CreateChatLogEntry(
            createTraceString = createTraceString,
            getCurrentTimeString = getCurrentTimeString
        )
    }

    @Test
    fun `test that logs with non null tags return null`() = runTest {
        val request = CreateLogEntryRequest(
            tag = "Tag",
            message = "message",
            priority = Log.DEBUG,
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
            priority = Log.DEBUG,
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
            priority = Log.DEBUG,
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
        val clientAppTag = "[clientApp]"

        val request = CreateLogEntryRequest(
            tag = null,
            message = "message",
            priority = Log.DEBUG,
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
            priority = Log.DEBUG,
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
            Log.VERBOSE to "VERBOSE",
            Log.DEBUG to "DEBUG",
            Log.INFO to "INFO",
            Log.ASSERT to "ASSERT",
            Log.WARN to "WARN",
            Log.ERROR to "ERROR",
            42 to "UNKNOWN",
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
            priority = Log.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.stackTrace).isEqualTo(traceString)
    }
}