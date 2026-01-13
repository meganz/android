package mega.privacy.android.feature.devicecenter.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.SettingsCameraUploadsNavKey
import mega.privacy.android.navigation.destination.SyncListNavKey
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey

class DeviceCenterFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            deviceCenterScreen(
                navigationHandler = navigationHandler,
                onNavigateToBackupFolder = { handle, errorMessage ->
                    navigationHandler.navigate(CloudDriveNavKey(handle))
                },
                onNavigateToNonBackupFolder = { handle, errorMessage ->
                    navigationHandler.navigate(CloudDriveNavKey(handle))
                },
                onNavigateToSyncs = {
                    navigationHandler.navigate(SyncListNavKey)
                },
                onNavigateToNewSync = { syncType: SyncType ->
                    navigationHandler.navigate(
                        SyncNewFolderNavKey(
                            syncType = syncType,
                            isFromDeviceCenter = true
                        )
                    )
                },
                onNavigateToCameraUploads = {
                    navigationHandler.navigate(SettingsCameraUploadsNavKey)
                }
            )
        }
}
