package mega.privacy.android.domain.entity

/**
 * Type of sync record
 */
enum class SyncRecordType(
    /**
     * sync record type value
     */
    val value: Int,
) {
    /**
     * type photo
     */
    TYPE_PHOTO(1),

    /**
     * type video
     */
    TYPE_VIDEO(2),

    /**
     * type any media
     */
    TYPE_ANY(-1);
}
