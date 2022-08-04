package mega.privacy.android.domain.entity

/**
 * Status of sync record
 */
enum class SyncStatus(
    /**
     * status value
     */
    val value: Int,
) {
    /**
     * status pending
     */
    STATUS_PENDING(0),

    /**
     * status compressing
     */
    STATUS_TO_COMPRESS(3);
}
