package mega.privacy.android.data.logging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import mega.privacy.android.data.gateway.LogWriterGateway
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.usecase.CreateLogEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogFileTreeTest {

    private class RecordingWriter(
        private val failOn: String? = null,
    ) : LogWriterGateway {
        val latch = CountDownLatch(1)
        val written = mutableListOf<String>()

        override fun writeLogEntry(logEntry: LogEntry) {
            if (logEntry.message == failOn) error("simulated write failure")
            written.add(logEntry.message)
            latch.countDown()
        }
    }

    @Test
    fun `test that the worker keeps writing after building an entry fails with an error`() {
        val writer = RecordingWriter()
        val createLogEntry = CreateLogEntry { request ->
            if (request.message == FAILING) throw OutOfMemoryError("simulated heap exhaustion")
            LogEntry(message = request.message, priority = request.priority.intValue)
        }
        val underTest = LogFileTree(Dispatchers.IO, createLogEntry, writer)

        underTest.d(FAILING)
        underTest.d(SURVIVOR)

        assertThat(writer.latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(writer.written).containsExactly(SURVIVOR)
    }

    @Test
    fun `test that the worker keeps writing after the writer fails`() {
        val writer = RecordingWriter(failOn = FAILING)
        val createLogEntry = CreateLogEntry { request ->
            LogEntry(message = request.message, priority = request.priority.intValue)
        }
        val underTest = LogFileTree(Dispatchers.IO, createLogEntry, writer)

        underTest.d(FAILING)
        underTest.d(SURVIVOR)

        assertThat(writer.latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        assertThat(writer.written).containsExactly(SURVIVOR)
    }

    private companion object {
        const val FAILING = "failing"
        const val SURVIVOR = "survivor"
        const val TIMEOUT_SECONDS = 5L
    }
}
