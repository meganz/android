package mega.privacy.android.feature.clouddrive.presentation.drivesync

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.sync.ui.permissions.SyncPermissionsManager
import javax.inject.Inject

/**
 * ViewModel for Drive Sync tabs screen
 */
@HiltViewModel
class DriveSyncViewModel @Inject constructor(
    val megaNavigator: MegaNavigator,
    val syncPermissionsManager: SyncPermissionsManager,
    val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

}
