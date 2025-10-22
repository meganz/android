package mega.privacy.android.feature.devicecenter.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.mobile.analytics.event.DeviceCenterItemClicked
import mega.privacy.mobile.analytics.event.DeviceCenterItemClickedEvent

class DeviceCenterFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            deviceCenterScreen(
                navigationHandler = navigationHandler,
                onNavigateToBackupFolder = { handle, errorMessage ->
                    // todo navigate to backup folder
                },
                onNavigateToNonBackupFolder = { handle, errorMessage ->
                    Analytics.tracker.trackEvent(
                        DeviceCenterItemClickedEvent(
                            DeviceCenterItemClicked.ItemType.Connection
                        )
                    )
                    navigationHandler.navigate(CloudDriveNavKey(handle))
                },
                onNavigateToSyncs = {
                    navigationHandler.navigate(DriveSyncNavKey(initialTabIndex = 1))
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
