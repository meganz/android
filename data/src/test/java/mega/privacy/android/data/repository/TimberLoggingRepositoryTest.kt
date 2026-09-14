package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import dagger.Lazy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileCompressionGateway
import mega.privacy.android.data.gateway.LogConfigurationGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.logging.LogFileTree
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import timber.log.Timber
import java.io.File

/**
 * SDK, Chat SDK and client app logs share a single file, so the repository plants a
 * single tree and cleans up the files left behind by the previous two file layout.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimberLoggingRepositoryTest {

    private lateinit var underTest: TimberLoggingRepository

    @TempDir
    lateinit var logDirectory: File

    private val testDispatcher = UnconfinedTestDispatcher()

    private val logConfigurationGateway = mock<LogConfigurationGateway>()

    // Only its identity matters here: the repository plants it and must not plant it twice.
    private val logFileTree = mock<LogFileTree>()

    @BeforeEach
    fun setUp() {
        Timber.uprootAll()
        underTest = TimberLoggingRepository(
            megaSdkLogger = mock<MegaLoggerInterface>(),
            megaChatLogger = mock<MegaChatLoggerInterface>(),
            logFileTree = Lazy { logFileTree },
            loggingConfig = logConfigurationGateway,
            fileCompressionGateway = mock<FileCompressionGateway>(),
            megaApiGateway = mock<MegaApiGateway>(),
            ioDispatcher = testDispatcher,
            logFileDirectory = Lazy { logDirectory },
            logZipFileDirectory = Lazy { logDirectory },
        )
    }

    @AfterEach
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `test that the log file tree is planted on initialise`() = runTest(testDispatcher) {
        underTest.initialise()

        assertThat(Timber.forest()).contains(logFileTree)
    }

    @Test
    fun `test that the log file tree is not planted twice`() = runTest(testDispatcher) {
        underTest.initialise()
        underTest.initialise()

        assertThat(Timber.forest().filter { it == logFileTree }).hasSize(1)
    }

    @Test
    fun `test that initialise succeeds when the log directory does not exist`() =
        runTest(testDispatcher) {
            val missingDirectory = File(logDirectory, "missing")
            underTest = TimberLoggingRepository(
                megaSdkLogger = mock<MegaLoggerInterface>(),
                megaChatLogger = mock<MegaChatLoggerInterface>(),
                logFileTree = Lazy { logFileTree },
                loggingConfig = logConfigurationGateway,
                fileCompressionGateway = mock<FileCompressionGateway>(),
                megaApiGateway = mock<MegaApiGateway>(),
                ioDispatcher = testDispatcher,
                logFileDirectory = Lazy { missingDirectory },
                logZipFileDirectory = Lazy { logDirectory },
            )

            underTest.initialise()

            assertThat(Timber.forest()).contains(logFileTree)
        }
}
