package mega.privacy.android.data.gateway

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.Keep
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatLoggerInterface
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat SDK logger
 *
 * Chat log listener that hands MEGAchat output to Timber, from where it reaches the log file
 * and, on debug builds, logcat. See logback.xml for the file configuration.
 *
 * MEGAchat also routes every MEGA SDK line into its own `megasdk` log channel, so this logger
 * receives a second copy of everything [TimberMegaLogger] already delivered. Those copies are
 * dropped here, before Timber, so no sink sees an SDK line twice and no work is spent
 * dispatching them. They are recognisable by MEGAchat's own prefix naming `sdk` as the
 * originating channel — `[<time>][sdk]`, or `[<time>][WRN][sdk]` once a level marker is
 * inserted for warnings and errors — and they are the lossy copies: the direct entry carries
 * the source line number, the forwarded one only the file. On a live session they were around
 * two thirds of all Chat SDK traffic, and nearly all of it during a large transfer.
 *
 * The same MEGAchat prefix is stripped from the lines that are kept. The Logback encoder adds
 * a timestamp and level to every line in the file, so leaving MEGAchat's own in place printed
 * both twice; the channel name (`[chatd]`, `[rtc]`, …) that follows is kept, as nothing else
 * supplies it. This mirrors [TimberMegaLogger], which drops the SDK's separate `time` argument
 * for the same reason.
 */
@Keep
internal class TimberChatLogger @Inject constructor() : MegaChatLoggerInterface {
    @SuppressLint("LogNotTimber")
    @Synchronized
    override fun log(loglevel: Int, message: String?) {
        try {
            val line = message?.let { stripMegaChatPrefix(it) ?: return }
            Timber.tag(TAG)
            when (loglevel) {
                MegaChatApi.LOG_LEVEL_MAX -> Timber.v(line)
                MegaChatApi.LOG_LEVEL_DEBUG -> Timber.d(line)
                MegaChatApi.LOG_LEVEL_INFO -> Timber.i(line)
                MegaChatApi.LOG_LEVEL_WARNING -> Timber.w(line)
                MegaChatApi.LOG_LEVEL_ERROR -> Timber.e(line)
                else -> Timber.i(line)
            }
        } catch (t: Throwable) {
            // swallow everything to protect native code
            Log.e(TAG, "Exception in SDK logger, swallowed to prevent SIGABRT", t)
        }
    }

    /**
     * Removes MEGAchat's `[MM-dd HH:mm:ss.SS]` timestamp and the optional `[WRN]` / `[ERR]`
     * level marker after it. Returns null when what follows is the `[sdk]` channel, i.e. a
     * forwarded MEGA SDK line that must be dropped.
     *
     * This runs on the SDK's native callback thread for every Chat SDK line — around 700 a
     * second during a large transfer — so it is a single pass of character checks with one
     * allocation for the result, not a regex. The timestamp is recognised by its separators,
     * which is enough to tell it from a line that starts with a channel name.
     */
    private fun stripMegaChatPrefix(message: String): String? {
        if (!startsWithTimestamp(message)) return message
        val timestampEnd = message.indexOf(']', TIMESTAMP_SEPARATORS_END)
        if (timestampEnd < 0) return message

        var pos = timestampEnd + 1
        if (hasLevelMarker(message, pos)) pos += LEVEL_MARKER_LENGTH
        if (message.startsWith(FORWARDED_SDK_CHANNEL, pos)) return null

        while (pos < message.length && message[pos] == ' ') pos++
        return message.substring(pos)
    }

    /** `[MM-dd HH:mm:ss` — the separators are at fixed offsets. */
    private fun startsWithTimestamp(message: String) =
        message.length > TIMESTAMP_SEPARATORS_END &&
            message[0] == '[' &&
            message[3] == '-' &&
            message[6] == ' ' &&
            message[9] == ':' &&
            message[12] == ':'

    /** `[WRN]` / `[ERR]` — three upper-case letters in brackets. */
    private fun hasLevelMarker(message: String, pos: Int) =
        pos + LEVEL_MARKER_LENGTH <= message.length &&
            message[pos] == '[' &&
            message[pos + 4] == ']' &&
            message[pos + 1].isUpperCaseLetter() &&
            message[pos + 2].isUpperCaseLetter() &&
            message[pos + 3].isUpperCaseLetter()

    private fun Char.isUpperCaseLetter() = this in 'A'..'Z'

    companion object {
        const val TAG = "[chat]"

        /** Index of the last fixed separator in `[MM-dd HH:mm:ss`. */
        private const val TIMESTAMP_SEPARATORS_END = 12

        private const val LEVEL_MARKER_LENGTH = 5

        /** MEGAchat's channel name for the MEGA SDK lines it forwards. */
        private const val FORWARDED_SDK_CHANNEL = "[sdk]"
    }
}
