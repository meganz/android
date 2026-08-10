package mega.privacy.android.feature.documentscanner.data.model

import java.io.IOException

/**
 * Errors thrown by [DownloadingScannerModelProvider] during the model download.
 *
 * Split into two cases so the worker can decide whether to retry:
 * - [Permanent] — the CDN says the resource is gone or malformed (HTTP 4xx
 *   other than the rate-limit / timeout codes). Retrying with the same URL
 *   won't help; the worker fails fast so the user can fall back to the legacy
 *   scanner instead of burning the retry budget on guaranteed failures.
 * - [Transient] — anything else (network blip, 5xx, integrity mismatch, write
 *   failure). The worker retries with the standard exponential backoff.
 */
internal sealed class ModelDownloadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause) {
    class Permanent(message: String, cause: Throwable? = null) :
        ModelDownloadException(message, cause)

    class Transient(message: String, cause: Throwable? = null) :
        ModelDownloadException(message, cause)
}
