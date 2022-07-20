package mega.privacy.android.domain.entity

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
    val status: Int = SyncStatus.STATUS_PENDING.value,
    val type: Int = 0,
    val nodeHandle: Long? = null,
    val isCopyOnly: Boolean = false,
    val isSecondary: Boolean = false,
)
