package mega.privacy.android.feature.sync.ui.synclist.folders

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.ui.model.SyncUiItem

internal data class SyncFoldersState(
    val syncUiItems: List<SyncUiItem>,
    val isRefreshing: Boolean = false,
    val isLowBatteryLevel: Boolean = false,
    val isStorageOverQuota: Boolean = false,
    val isFreeAccount: Boolean = true,
    val isLoading: Boolean = false,
    val showSyncsPausedErrorDialog: Boolean = false,
    val showConfirmRemoveSyncFolderDialog: Boolean = false,
    val syncUiItemToRemove: SyncUiItem? = null,
    @StringRes val snackbarMessage: Int? = null,
)
