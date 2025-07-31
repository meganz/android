package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal data class SyncFoldersUiState(
    val syncUiItems: List<SyncUiItem>,
    val isRefreshing: Boolean = false,
    val isLowBatteryLevel: Boolean = false,
    val isStorageOverQuota: Boolean = false,
    val isLoading: Boolean = false,
    val showConfirmRemoveSyncFolderDialog: Boolean = false,
    val isDisableBatteryOptimizationEnabled: Boolean = false,
    val syncUiItemToRemove: SyncUiItem? = null,
    val movedFolderName: String? = null,
    @StringRes val snackbarMessage: Int? = null,
) {
    val isWarningBannerDisplayed =
        (syncUiItems.isNotEmpty() && isLowBatteryLevel) || isStorageOverQuota
}
