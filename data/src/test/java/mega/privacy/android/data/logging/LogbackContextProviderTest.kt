package mega.privacy.android.data.logging

import ch.qos.logback.classic.LoggerContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.ILoggerFactory
import org.slf4j.helpers.SubstituteLoggerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogbackContextProviderTest {

    private class RecordingFactory(
        private val factory: ILoggerFactory,
    ) : () -> ILoggerFactory {
        var invocations = 0
            private set

        override fun invoke(): ILoggerFactory {
            invocations++
            return factory
        }
    }

    @Test
    fun `test that ifConfigured does not ask for the logger factory before configure`() = runTest {
        val loggerFactory = RecordingFactory(LoggerContext())
        val underTest = LogbackContextProvider(loggerFactory)
        var blockRan = false

        underTest.ifConfigured { blockRan = true }

        assertThat(blockRan).isFalse()
        assertThat(loggerFactory.invocations).isEqualTo(0)
    }

    @Test
    fun `test that ifConfigured runs the block when configure succeeded`() = runTest {
        val underTest = LogbackContextProvider { LoggerContext() }
        underTest.configure { true }
        var blockRan = false

        underTest.ifConfigured { blockRan = true }

        assertThat(blockRan).isTrue()
    }

    @Test
    fun `test that ifConfigured does not run the block when configure reported a failure`() =
        runTest {
            val underTest = LogbackContextProvider { LoggerContext() }
            underTest.configure { false }
            var blockRan = false

            underTest.ifConfigured { blockRan = true }

            assertThat(blockRan).isFalse()
        }

    @Test
    fun `test that configure returns false when logback is not the bound provider`() = runTest {
        val underTest = LogbackContextProvider { SubstituteLoggerFactory() }
        var blockRan = false

        val result = underTest.configure {
            blockRan = true
            true
        }

        assertThat(result).isFalse()
        assertThat(blockRan).isFalse()
    }

    @Test
    fun `test that configure returns true and passes the logback context to the block`() = runTest {
        val loggerContext = LoggerContext()
        val underTest = LogbackContextProvider { loggerContext }
        var received: LoggerContext? = null

        val result = underTest.configure {
            received = it
            true
        }

        assertThat(result).isTrue()
        assertThat(received).isSameInstanceAs(loggerContext)
    }
}
