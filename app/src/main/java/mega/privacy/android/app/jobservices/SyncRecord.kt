package mega.privacy.android.app.jobservices

const val STATUS_PENDING = 0
const val STATUS_TO_COMPRESS = 3

const val TYPE_PHOTO = 1
const val TYPE_VIDEO = 2
const val TYPE_ANY = -1

data class SyncRecord(
    val id: Int = 0,
    val localPath: String? = null,
    var newPath: String? = null,
    val originFingerprint: String? = null,
    val newFingerprint: String? = null,
    val timestamp: Long? = null,
    var fileName: String? = null,
    val longitude: Float? = null,
    val latitude: Float? = null,
    val status: Int = STATUS_PENDING,
    val type: Int = 0,
    val nodeHandle: Long? = null,
    val isCopyOnly: Boolean = false,
    val isSecondary: Boolean = false
)
