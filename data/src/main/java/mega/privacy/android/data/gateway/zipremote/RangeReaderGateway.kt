package mega.privacy.android.data.gateway.zipremote

import java.io.File

/**
 * Reads part of a file served over http, which is how the SDK's local streaming server hands out
 * a cloud file without downloading all of it.
 */
internal interface RangeReaderGateway {

    /**
     * Copies [length] bytes starting at [start] of [url] into [destination] at the same offset.
     *
     * Cancelling the caller closes the connection and returns once the copy has stopped, so
     * nothing is written to [destination] after this has thrown.
     *
     * @param onBytes called on an IO thread for each chunk written, with its size; it must be
     * fast and thread-safe
     * @throws java.io.IOException when the server does not return exactly that range
     */
    suspend fun copyRange(
        url: String,
        start: Long,
        length: Long,
        destination: File,
        onBytes: (Long) -> Unit = {},
    )
}
