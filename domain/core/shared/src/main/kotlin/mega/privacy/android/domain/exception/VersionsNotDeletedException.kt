package mega.privacy.android.domain.exception

/**
 * Thrown when not all requested versions to be deleted has been deleted
 * @param totalRequestedToDelete the amount of versions requested to be deleted
 * @param totalNotDeleted the amount of versions not being deleted
 */
class VersionsNotDeletedException(
    val totalRequestedToDelete: Int,
    val totalNotDeleted: Int,
) : RuntimeException("Not all versions have been deleted")