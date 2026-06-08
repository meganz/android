package mega.privacy.android.data.logging

import ch.qos.logback.core.rolling.RollingFileAppender

/**
 * RollingFileAppender that exposes a [flushNow] method so callers can force
 * the underlying [java.io.OutputStream] to flush its buffered bytes to the OS
 * without closing the appender.
 *
 * Used together with `<immediateFlush>false</immediateFlush>` to batch writes
 * while still allowing manual flushes at lifecycle boundaries (e.g. when the
 * app enters the background) so logs are not lost when the process is killed.
 */
class FlushableRollingFileAppender<E> : RollingFileAppender<E>() {

    fun flushNow() {
        lock.lock()
        try {
            outputStream?.flush()
        } catch (_: Throwable) {
            // Ignore: flushing is best-effort. Any I/O failure here would also
            // surface on the next regular write and be reported via Logback's
            // status manager.
        } finally {
            lock.unlock()
        }
    }
}
