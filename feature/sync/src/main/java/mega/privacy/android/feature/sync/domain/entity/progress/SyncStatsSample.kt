package mega.privacy.android.feature.sync.domain.entity.progress

/**
 * One MegaSyncStats sample. Pending counts include files in flight.
 *
 * @property isScanning True while the sync scans its folders
 * @property isSyncing True while the SDK reports the sync as syncing; not work on its own
 * @property pendingUploads Files waiting to upload, including those in flight
 * @property pendingDownloads Files waiting to download, including those in flight
 */
internal data class SyncStatsSample(
    val isScanning: Boolean,
    val isSyncing: Boolean,
    val pendingUploads: Int,
    val pendingDownloads: Int,
) {
    val pendingItems: Int get() = pendingUploads + pendingDownloads
}
