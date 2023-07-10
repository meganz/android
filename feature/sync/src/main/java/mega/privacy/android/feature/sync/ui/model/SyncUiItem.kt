package mega.privacy.android.feature.sync.ui.model

internal data class SyncUiItem(
    val id: Long,
    val folderPairName: String,
    val status: SyncStatus,
    val deviceStoragePath: String,
    val megaStoragePath: String,
    val method: String,
    val expanded: Boolean,
)