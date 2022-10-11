package mega.privacy.android.data.logging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.usecase.CreateLogEntry
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class LogFlowTreeTest {
    private lateinit var underTest: LogFlowTree

    private val createLogEntry = mock<CreateLogEntry> {
        onBlocking { invoke(any()) }.thenReturn(
            LogEntry(message = "", priority = 0))
    }

    @Before
    fun setUp() {
        underTest = LogFlowTree(
            dispatcher = UnconfinedTestDispatcher(),
            createLogEntry = createLogEntry
        )
    }

    @Test
    fun `test that correct ignored classes are passed to create entry`() = runTest {
        val expected = listOf<String>(
            Timber::class.java.name,
            Timber::class.java.name,
            Timber.Tree::class.java.name,
            Timber.DebugTree::class.java.name,
            LogFlowTree::class.java.name,
        )
        underTest.d("Message")

        verifyBlocking(createLogEntry) { invoke(argForWhich { loggingClasses.containsAll(expected) }) }
    }

}