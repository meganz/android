package mega.privacy.android.data.gateway

import android.util.Log
import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaChatApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber

/**
 * MEGAchat re-delivers every MEGA SDK log line through its own logger, and prefixes every
 * line with its own timestamp and level. Both are dealt with here, before Timber: the SDK
 * copies are dropped so no sink sees an SDK line twice, and the prefix is stripped so the
 * Logback encoder's timestamp and level are the only ones in the file.
 */
internal class TimberChatLoggerTest {

    private class RecordingTree : Timber.Tree() {
        data class Entry(val priority: Int, val tag: String?, val message: String)

        val entries = mutableListOf<Entry>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            entries += Entry(priority, tag, message)
        }
    }

    private lateinit var underTest: TimberChatLogger
    private lateinit var tree: RecordingTree

    @BeforeEach
    fun setUp() {
        Timber.uprootAll()
        tree = RecordingTree()
        Timber.plant(tree)
        underTest = TimberChatLogger()
    }

    @AfterEach
    fun tearDown() {
        Timber.uprootAll()
    }

    private fun logged() = tree.entries.map { it.message }

    @Test
    fun `test that sdk lines forwarded by megachat are not passed to timber`() {
        underTest.log(
            MegaChatApi.LOG_LEVEL_DEBUG,
            "[09-08 04:53:19.941][sdk] (Req#14) cs POST target URL: https://g.api.mega.co.nz/cs",
        )

        assertThat(tree.entries).isEmpty()
    }

    @Test
    fun `test that forwarded sdk lines are dropped at every level`() {
        underTest.log(MegaChatApi.LOG_LEVEL_WARNING, "[09-08 05:04:35.104][WRN][sdk] Request (GET_USER_DATA) finished with error")
        underTest.log(MegaChatApi.LOG_LEVEL_ERROR, "[09-08 05:04:35.136][ERR][sdk] Unable to FileAccess::fopen")

        assertThat(tree.entries).isEmpty()
    }

    @Test
    fun `test that the megachat timestamp is stripped and the channel kept`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.241][rtc] Creating WebRtcContext")

        assertThat(logged()).containsExactly("[rtc] Creating WebRtcContext")
    }

    @Test
    fun `test that the megachat level marker is stripped together with the timestamp`() {
        underTest.log(MegaChatApi.LOG_LEVEL_WARNING, "[09-08 05:04:35.104][WRN][chatd] Reconnecting to chatd")
        underTest.log(MegaChatApi.LOG_LEVEL_ERROR, "[09-08 05:04:35.136][ERR][chatd] Connection lost")

        assertThat(logged()).containsExactly("[chatd] Reconnecting to chatd", "[chatd] Connection lost").inOrder()
    }

    @Test
    fun `test that lines without a channel keep their text after the timestamp`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.319] Karere log debug: db opened")

        assertThat(logged()).containsExactly("Karere log debug: db opened")
    }

    @Test
    fun `test that lines without a megachat prefix are passed unchanged`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "plain message with no prefix")

        assertThat(logged()).containsExactly("plain message with no prefix")
    }

    @Test
    fun `test that a line starting with a channel but no timestamp is passed unchanged`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[chatd] no timestamp on this one")

        assertThat(logged()).containsExactly("[chatd] no timestamp on this one")
    }

    @Test
    fun `test that short or malformed lines are passed unchanged`() {
        val lines = listOf("", "[", "[09-08", "[09-08 04:53:15", "[09-08 04:53:15.2")

        lines.forEach { underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, it) }

        // Timber itself swallows the empty string, so it is the only one not recorded.
        assertThat(logged()).containsExactlyElementsIn(lines.filter { it.isNotEmpty() }).inOrder()
    }

    @Test
    fun `test that a truncated level marker after a timestamp is left in place`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.241][WR")

        // The timestamp is a complete prefix and goes; the rest is not a level marker and stays.
        assertThat(logged()).containsExactly("[WR")
    }

    @Test
    fun `test that a line that is only a prefix does not crash`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.241]")
        underTest.log(MegaChatApi.LOG_LEVEL_WARNING, "[09-08 04:53:15.241][WRN]")

        // Nothing left to log once the prefix is gone; Timber drops the empty message.
        assertThat(tree.entries).isEmpty()
    }

    @Test
    fun `test that lines from every megachat channel are passed to timber`() {
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.241][rtc] a")
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.323][chatd] b")
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.314][megachatapi] c")
        underTest.log(MegaChatApi.LOG_LEVEL_DEBUG, "[09-08 04:53:15.400][websockets] d")

        assertThat(logged()).containsExactly("[rtc] a", "[chatd] b", "[megachatapi] c", "[websockets] d").inOrder()
    }

    @Test
    fun `test that passed lines carry the chat tag`() {
        underTest.log(MegaChatApi.LOG_LEVEL_INFO, "[09-08 04:53:15.241][rtc] Creating WebRtcContext")

        assertThat(tree.entries.single().tag).isEqualTo(TimberChatLogger.TAG)
    }

    @Test
    fun `test that megachat log levels map to android priorities`() {
        val expected = mapOf(
            MegaChatApi.LOG_LEVEL_MAX to Log.VERBOSE,
            MegaChatApi.LOG_LEVEL_DEBUG to Log.DEBUG,
            MegaChatApi.LOG_LEVEL_INFO to Log.INFO,
            MegaChatApi.LOG_LEVEL_WARNING to Log.WARN,
            MegaChatApi.LOG_LEVEL_ERROR to Log.ERROR,
        )

        expected.keys.forEach { underTest.log(it, "[09-08 04:53:15.241][chatd] message") }

        assertThat(tree.entries.map { it.priority }).containsExactlyElementsIn(expected.values).inOrder()
    }
}
