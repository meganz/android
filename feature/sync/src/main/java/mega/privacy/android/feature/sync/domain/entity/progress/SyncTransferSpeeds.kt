package mega.privacy.android.feature.sync.domain.entity.progress

/** Bytes per second; null while that direction has no pending sync transfer. */
internal data class SyncTransferSpeeds(val uploadSpeed: Long?, val downloadSpeed: Long?)
