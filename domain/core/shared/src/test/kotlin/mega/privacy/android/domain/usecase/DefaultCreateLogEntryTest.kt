package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.logging.CreateLogEntryRequest
import mega.privacy.android.domain.entity.logging.LogPriority
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/**
 * Tests for the single [CreateLogEntry] implementation that feeds the one merged
 * log file. Both SDK and Chat SDK entries must survive and stay identifiable, since
 * they now share a file with each other and with the client app entries.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultCreateLogEntryTest {
    private lateinit var underTest: CreateLogEntry

    private val traceString = "Trace string"

    private val createTraceString = mock<CreateTraceString>()

    private val appVersion = "v1"

    private val environmentRepository = mock<EnvironmentRepository>()

    // The use case caches the app version, so every test needs a fresh instance.
    @BeforeEach
    fun setUp() {
        reset(createTraceString, environmentRepository)
        createTraceString.stub { onBlocking { invoke(any(), any()) }.thenReturn(traceString) }
        whenever(environmentRepository.getAppInfo()).thenReturn(AppInfo(appVersion, null))
        underTest = DefaultCreateLogEntry(
            createTraceString = createTraceString,
            environmentRepository = environmentRepository,
        )
    }

    private fun request(
        tag: String?,
        message: String = "message",
        priority: LogPriority = LogPriority.DEBUG,
        throwable: Throwable? = null,
    ) = CreateLogEntryRequest(
        tag = tag,
        message = message,
        priority = priority,
        throwable = throwable,
        trace = emptyList(),
        loggingClasses = emptyList(),
        sdkLoggers = emptyList(),
    )

    @Test
    fun `test that sdk logs are not discarded`() = runTest {
        assertThat(underTest(request(tag = "[sdk]"))).isNotNull()
    }

    @Test
    fun `test that chat sdk logs are not discarded`() = runTest {
        assertThat(underTest(request(tag = "[chat]"))).isNotNull()
    }

    @Test
    fun `test that sdk logs keep the sdk tag`() = runTest {
        assertThat(underTest(request(tag = "[sdk]"))?.tag).isEqualTo("[sdk]")
    }

    @Test
    fun `test that chat sdk logs keep the chat sdk tag`() = runTest {
        assertThat(underTest(request(tag = "[chat]"))?.tag).isEqualTo("[chat]")
    }

    @Test
    fun `test that sdk logs have no stack trace added`() = runTest {
        assertThat(underTest(request(tag = "[sdk]"))?.stackTrace).isNull()
    }

    @Test
    fun `test that chat sdk logs have no stack trace added`() = runTest {
        assertThat(underTest(request(tag = "[chat]"))?.stackTrace).isNull()
    }

    @Test
    fun `test that a non null non sdk tag is used as is`() = runTest {
        assertThat(underTest(request(tag = "TAG"))?.tag).isEqualTo("TAG")
    }

    @Test
    fun `test that a null tag uses the client app tag with the app version`() = runTest {
        assertThat(underTest(request(tag = null))?.tag).isEqualTo("[clientApp $appVersion]")
    }

    @Test
    fun `test that client app logs have a stack trace added`() = runTest {
        assertThat(underTest(request(tag = null))?.stackTrace).isEqualTo(traceString)
    }

    @Test
    fun `test that message priority and throwable are passed through`() = runTest {
        val throwable = Throwable()

        val actual = underTest(
            request(
                tag = null,
                message = "a message",
                priority = LogPriority.WARN,
                throwable = throwable,
            )
        )

        assertThat(actual?.message).isEqualTo("a message")
        assertThat(actual?.priority).isEqualTo(LogPriority.WARN.intValue)
        assertThat(actual?.throwable).isEqualTo(throwable)
    }

    @Test
    fun `test that app version is only fetched once`() = runTest {
        assertThat(underTest(request(tag = null))?.tag).contains(appVersion)

        verify(environmentRepository).getAppInfo()

        assertThat(underTest(request(tag = null))?.tag).contains(appVersion)

        verifyNoMoreInteractions(environmentRepository)
    }
}
