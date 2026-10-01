package mega.privacy.android.domain.exception

/**
 * Thrown when remote content exceeds the size the caller is willing to hold in memory.
 *
 * @property maxBytes the limit that was exceeded
 */
class DataTooLargeException(val maxBytes: Int) :
    RuntimeException("Content exceeds the $maxBytes byte limit")
