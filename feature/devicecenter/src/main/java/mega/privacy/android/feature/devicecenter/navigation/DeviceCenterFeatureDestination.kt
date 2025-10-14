package mega.privacy.android.feature.devicecenter.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class DeviceCenterFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            deviceCenterScreen(
                navigationHandler = navigationHandler,
                onNavigateToBackupFolder = { handle, errorMessage ->
                    // TODO: Navigate to backup folder in cloud drive
                },
                onNavigateToNonBackupFolder = { handle, errorMessage ->
                    // TODO: Navigate to non-backup folder in cloud drive
                },
                onNavigateToSyncs = {
                    // TODO: Navigate to syncs screen
                },
                onNavigateToNewSync = { syncType: SyncType ->
                    // TODO: Navigate to new sync screen
                },
                onNavigateToCameraUploads = {
                    // TODO: Navigate to camera uploads settings
                }
            )
        }
}
