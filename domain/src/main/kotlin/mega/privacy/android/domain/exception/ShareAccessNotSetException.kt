package mega.privacy.android.domain.exception

/**
 * Thrown when not all requested share access have been set
 * @param totalNotSet the amount of users not being set
 */
class ShareAccessNotSetException(
    val totalNotSet: Int,
) : RuntimeException("Not all share access have been set")