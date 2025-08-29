package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.uri.UriPath

internal data class SyncDebris(
    val syncId: Long,
    val path: UriPath,
    val sizeInBytes: Long,
)
