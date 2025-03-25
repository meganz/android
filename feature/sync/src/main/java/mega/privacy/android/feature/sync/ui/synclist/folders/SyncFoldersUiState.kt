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
    val syncUiItemToRemove: SyncUiItem? = null,
    @StringRes val snackbarMessage: Int? = null,
)
