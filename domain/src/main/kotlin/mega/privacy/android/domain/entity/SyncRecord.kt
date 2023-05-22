package mega.privacy.android.domain.entity

/**
 * These records are saved into the database and used for camera upload
 */
data class SyncRecord(
    /**
     * record ID
     */
    val id: Int = 0,
    /**
     * record path locally
     */
    val localPath: String?,
    /**
     * new record path
     */
    var newPath: String?,
    /**
     * fingerprint locally
     */
    val originFingerprint: String?,
    /**
     * new record fingerprint
     */
    val newFingerprint: String?,
    /**
     * timestamp of media
     */
    val timestamp: Long?,
    /**
     * name of record
     */
    var fileName: String?,
    /**
     * GPS longitude coordinate
     */
    val longitude: Float?,
    /**
     * GPS latitude coordinate
     */
    val latitude: Float?,
    /**
     * record status (ready for upload or compression)
     */
    val status: Int,
    /**
     * record type (photo or video or anything)
     */
    val type: SyncRecordType,
    /**
     * node handle of record
     */
    val nodeHandle: Long?,
    /**
     * if node exists or not
     */
    val isCopyOnly: Boolean,
    /**
     * secondary media or not
     */
    val isSecondary: Boolean,
)
