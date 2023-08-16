package mega.privacy.android.domain.entity

/**
 * These records are saved into the database and used for camera upload
 *
 * @property id record ID from the database
 * @property localPath
 * @property newPath a new path in case we need to create a temporary file for extracting the gps data or a compressed video
 * @property originFingerprint the original fingerprint retrieved from a Node or calculated locally
 * @property newFingerprint the current fingerprint retrieved from a Node
 * @property timestamp a timestamp corresponding to the max of added / modified time of the file
 * @property fileName
 * @property longitude GPS longitude coordinate
 * @property latitude GPS latitude coordinate
 * @property status ready for upload or need compression
 * @property type photo or video
 * @property nodeHandle the possible nodeHandle in case we need to copy the file from another Node
 * @property isCopyOnly true if the node already exists in the cloud drive
 * @property isSecondary true if secondary media
 */
data class SyncRecord(
    val id: Int = 0,
    val localPath: String,
    var newPath: String?,
    val originFingerprint: String?,
    val newFingerprint: String?,
    val timestamp: Long,
    val fileName: String,
    val longitude: Float?,
    val latitude: Float?,
    val status: Int,
    val type: SyncRecordType,
    val nodeHandle: Long?,
    val isCopyOnly: Boolean,
    val isSecondary: Boolean,
)
