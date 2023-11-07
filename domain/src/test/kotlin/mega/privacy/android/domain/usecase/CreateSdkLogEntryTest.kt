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
class CreateSdkLogEntryTest {
    private lateinit var underTest: CreateLogEntry

    private val traceString = "Trace string"

    private val createTraceString = mock<CreateTraceString> {
        onBlocking { invoke(any(), any()) }.thenReturn(
            traceString
        )
    }

    private val appVersion = "v1"

    private val environmentRepository = mock<EnvironmentRepository> {
        onBlocking { getAppInfo() }.thenReturn(AppInfo(appVersion, null))
    }

    @Before
    fun setUp() {
        underTest = CreateSdkLogEntry(
            createTraceString = createTraceString,
            environmentRepository = environmentRepository,
        )
    }

    @Test
    fun `test that non null tag is used in result`() = runTest {
        val tag = "TAG"
        val request = CreateLogEntryRequest(
            tag = tag,
            message = "message",
            priority = LogPriority.DEBUG,
            throwable = null,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.tag).isEqualTo(tag)
    }

    @Test
    fun `test that null tag uses client app tag`() = runTest {
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

        assertThat(actual?.tag).startsWith(clientAppTag)
    }

    @Test
    fun `test that trace from loggers with no tag returns null`() = runTest {
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

        assertThat(actual).isNull()
    }

    @Test
    fun `test that values match`() = runTest {
        val message = "message"
        val priority = LogPriority.DEBUG
        val throwable = Throwable()
        val request = CreateLogEntryRequest(
            tag = null,
            message = message,
            priority = priority,
            throwable = throwable,
            trace = emptyList(),
            loggingClasses = emptyList(),
            sdkLoggers = emptyList()
        )
        val actual = underTest(request)

        assertThat(actual?.message).isEqualTo(message)
        assertThat(actual?.priority).isEqualTo(priority.intValue)
        assertThat(actual?.throwable).isEqualTo(throwable)
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