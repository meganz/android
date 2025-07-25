package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * ViewModel for Drive Sync tabs screen
 */
@HiltViewModel
class DriveSyncViewModel @Inject constructor(
    val megaNavigator: MegaNavigator,
    val syncPermissionsManager: SyncPermissionsManager,
) : ViewModel() {

}