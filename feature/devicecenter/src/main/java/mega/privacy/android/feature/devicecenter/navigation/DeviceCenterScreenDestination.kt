package mega.privacy.android.feature.devicecenter.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.devicecenter.ui.lists.loading.DeviceCenterLoadingScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.DeviceCenterNavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderBuilder<NavKey>.deviceCenterScreen(
    navigationHandler: NavigationHandler,
    onNavigateToBackupFolder: (handle: Long, errorMessage: Int?) -> Unit,
    onNavigateToNonBackupFolder: (handle: Long, errorMessage: Int?) -> Unit,
    onNavigateToSyncs: () -> Unit,
    onNavigateToNewSync: (syncType: SyncType) -> Unit,
    onNavigateToCameraUploads: () -> Unit,
) {
    entry<DeviceCenterNavKey> {
        // todo add M3 Screen content for Device Center
        DeviceCenterLoadingScreen()
    }
}
