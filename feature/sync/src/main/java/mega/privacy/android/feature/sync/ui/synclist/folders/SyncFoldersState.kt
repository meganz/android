package mega.privacy.android.feature.sync.ui.synclist.folders

import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal data class SyncFoldersState(
    val syncUiItems: List<SyncUiItem>,
    val isRefreshing: Boolean = false,
    val isLowBatteryLevel: Boolean = false,
    val isStorageOverQuota: Boolean = false,
    val isFreeAccount: Boolean = true,
)
