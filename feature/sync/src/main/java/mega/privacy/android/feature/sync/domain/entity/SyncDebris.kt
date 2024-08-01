package mega.privacy.android.feature.sync.domain.entity

internal data class SyncDebris(
    val syncId: Long,
    val path: String,
    val sizeInBytes: Long,
)