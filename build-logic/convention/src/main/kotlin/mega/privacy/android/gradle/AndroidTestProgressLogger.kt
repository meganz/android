package mega.privacy.android.gradle

import java.io.File
import java.io.IOException
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

private const val LOG_PREFIX = "[androidTest]"
private const val LOGCAT_PREFIX = "logcat-"
private const val LOGCAT_SUFFIX = ".txt"
private const val EXIT_CODE_FILE = "test-result-exit-code.txt"
private const val TEST_STARTED_MARKER = "TestRunner: started:"
private const val POLL_INTERVAL_MS = 500L
private const val SHUTDOWN_TIMEOUT_MS = 2_000L

/**
 * Reports which instrumented test a Gradle Managed Device task is currently running.
 *
 * AGP runs the tests inside a process-isolated worker, so the task prints nothing until it
 * finishes. It does stream each test's logcat into its own `logcat-<class>-<method>.txt` under the
 * results directory, which it empties when it starts. With the test orchestrator enabled, an
 * enumeration pass creates all of those files before the first test runs, and a file is then
 * truncated and rewritten the moment its test starts. So a change to a file's first line means
 * execution reached that test, and the file count at that point is the suite size.
 *
 * A test short enough that both writes land inside one poll is not seen changing, so the run is
 * reconciled against the full file list at the end and anything still unreported is emitted then.
 * Files left over from an earlier run are ignored until this run rewrites them: the task empties
 * the directory in its action, which is after the `doFirst` that starts this monitor.
 */
internal class AndroidTestProgressMonitor(
    private val resultsDir: File,
    private val testListFile: File,
    private val log: (String) -> Unit,
) {
    private val startedAt = System.currentTimeMillis()
    private val firstLines = mutableMapOf<String, String>()
    private val startedTests = mutableSetOf<String>()
    private var total = 0
    private var reported = 0

    @Volatile
    private var stopped = false

    fun stop() {
        stopped = true
    }

    fun run() {
        while (true) {
            val finished = File(resultsDir, EXIT_CODE_FILE).isFromThisRun()
            scan()
            if (finished || stopped) break
            try {
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        finish()
    }

    private fun scan() {
        val files = logcatFiles() ?: return

        val transitioned = mutableListOf<String>()
        for (file in files) {
            val firstLine = file.readFirstLine() ?: continue
            val previous = firstLines.put(file.name, firstLine)
            if (previous != null && previous != firstLine && startedTests.add(file.name)) {
                transitioned += file.name
            }
        }
        if (transitioned.isEmpty()) return

        if (total == 0) {
            total = firstLines.size
            writeTestList()
        }
        if (startedTests.size > total) total = startedTests.size
        transitioned.forEach(::report)
    }

    /** Emit anything the polling never caught changing, so the tally is always complete. */
    private fun finish() {
        val pending = firstLines.keys.filterNot { it in startedTests }
        if (pending.isNotEmpty()) {
            if (total == 0) {
                total = firstLines.size
                writeTestList()
            }
            total = maxOf(total, startedTests.size + pending.size)
            pending.sortedBy { firstLines.getValue(it) }.forEach {
                startedTests += it
                report(it)
            }
        }
        if (reported > 0) log("$LOG_PREFIX done, $reported/$total")
    }

    private fun logcatFiles(): List<File>? = resultsDir.listFiles { file: File ->
        file.name.startsWith(LOGCAT_PREFIX) && file.name.endsWith(LOGCAT_SUFFIX) && file.isFromThisRun()
    }?.sortedBy { it.name }

    /**
     * The task empties the results directory in its action, which runs after the `doFirst` that
     * starts this monitor, so everything still on disk at that point is the previous run's and must
     * not be mistaken for this one's -- least of all its `test-result-exit-code.txt`, which would
     * end the run before it began.
     */
    private fun File.isFromThisRun() = isFile && lastModified() >= startedAt

    private fun report(fileName: String) {
        reported++
        val index = reported.toString().padStart(total.toString().length)
        log("$LOG_PREFIX [$index/$total] ${displayNameOf(fileName)}")
    }

    private fun writeTestList() {
        val classes = firstLines.keys.map { classNameOf(it) }.distinct().size
        val testLabel = if (total == 1) "test" else "tests"
        val classLabel = if (classes == 1) "class" else "classes"
        log("$LOG_PREFIX $total $testLabel, $classes $classLabel -> ${testListFile.absolutePath}")
        try {
            testListFile.parentFile?.mkdirs()
            testListFile.writeText(
                firstLines.keys.sorted()
                    .joinToString(separator = "\n", postfix = "\n") { filterNameOf(it) }
            )
        } catch (error: IOException) {
            log("$LOG_PREFIX could not write the test list: ${error.message}")
        }
    }

    private fun bodyOf(fileName: String) =
        fileName.removePrefix(LOGCAT_PREFIX).removeSuffix(LOGCAT_SUFFIX)

    private fun classNameOf(fileName: String): String {
        val body = bodyOf(fileName)
        val separator = body.lastIndexOf('-')
        return if (separator <= 0) body else body.substring(0, separator)
    }

    /** `mega.privacy.android.app.HomeScreenTest#recentVideoFileShownUnderToday`, ready to be
     * pasted into `-Pandroid.testInstrumentationRunnerArguments.class=`. */
    private fun filterNameOf(fileName: String): String {
        val body = bodyOf(fileName)
        val separator = body.lastIndexOf('-')
        return if (separator <= 0) body else "${body.substring(0, separator)}#${body.substring(separator + 1)}"
    }

    private fun displayNameOf(fileName: String): String {
        val body = bodyOf(fileName)
        val separator = body.lastIndexOf('-')
        if (separator <= 0) return body
        val simpleName = body.substring(0, separator).substringAfterLast('.')
        return "$simpleName.${body.substring(separator + 1)}"
    }

    /**
     * Only a complete `started:` record counts: a read can land while AGP is rewriting the file and
     * come back with a truncated line, which would otherwise look like the test starting twice.
     */
    private fun File.readFirstLine(): String? = try {
        useLines { lines -> lines.firstOrNull() }?.takeIf { it.contains(TEST_STARTED_MARKER) }
    } catch (error: IOException) {
        null
    }
}

/**
 * Owns the progress monitor threads so they are stopped at the end of the build even when the test
 * task fails, rather than being left behind in a reused Gradle daemon.
 */
abstract class AndroidTestProgressService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    private val monitors = mutableListOf<Pair<AndroidTestProgressMonitor, Thread>>()

    /**
     * Start reporting progress for one managed device test run.
     *
     * @param resultsDir the task's results directory, where the per-test logcat files appear
     * @param testListFile where the enumerated suite is written once it is known
     * @param log sink for the progress lines
     */
    fun start(resultsDir: File, testListFile: File, log: (String) -> Unit) {
        val monitor = AndroidTestProgressMonitor(resultsDir, testListFile, log)
        val thread = Thread(monitor::run, "mega-android-test-progress").apply { isDaemon = true }
        synchronized(monitors) { monitors += monitor to thread }
        thread.start()
    }

    override fun close() {
        val running = synchronized(monitors) { monitors.toList().also { monitors.clear() } }
        running.forEach { (monitor, _) -> monitor.stop() }
        // Bounded so a wedged monitor can never hold up the end of the build.
        running.forEach { (_, thread) -> thread.join(SHUTDOWN_TIMEOUT_MS) }
    }
}
