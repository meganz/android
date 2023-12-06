package mega.privacy.android.feature.sync.ui.model

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.domain.entity.SyncStatus

internal data class SyncUiItem(
    val id: Long,
    val folderPairName: String,
    val status: SyncStatus,
    val hasStalledIssues: Boolean,
    val deviceStoragePath: String,
    val megaStoragePath: String,
    @StringRes val method: Int,
    val expanded: Boolean,
)