package mega.privacy.android.data.gateway

/**
 * Forces buffered log data to be written out to the OS.
 *
 * The file appenders configured in `logback.xml` run with
 * `<immediateFlush>false</immediateFlush>`, which means writes accumulate in an
 * 8KB OutputStream buffer until it fills up. Calling [flush] at lifecycle
 * boundaries (e.g. when the app enters the background) reduces the amount of
 * log data lost if the OS kills the process before the next automatic flush.
 */
interface LogFlushGateway {
    suspend fun flush()
}
