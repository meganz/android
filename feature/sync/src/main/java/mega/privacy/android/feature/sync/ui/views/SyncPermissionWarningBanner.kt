package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import mega.privacy.android.shared.original.core.ui.utils.ComposableLifecycle

/**
 * Permission banner shown on top of sync screens
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SyncPermissionWarningBanner(
    syncPermissionsManager: SyncPermissionsManager,
    isDisableBatteryOptimizationEnabled: Boolean,
) {
    var hasUnrestrictedBatteryUsage: Boolean? by rememberSaveable { mutableStateOf(null) }

    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            hasUnrestrictedBatteryUsage =
                syncPermissionsManager.isDisableBatteryOptimizationGranted()
        }
    }

    if (isDisableBatteryOptimizationEnabled) {
        hasUnrestrictedBatteryUsage?.let { hasUnrestrictedBatteryUsageValue ->
            if (hasUnrestrictedBatteryUsageValue.not()) {
                WarningBanner(
                    textString = stringResource(id = R.string.sync_battery_optimisation_banner),
                    onCloseClick = null,
                    modifier = Modifier.clickable {
                        syncPermissionsManager.launchAppSettingBatteryOptimisation()
                    },
                )
            }
        }
    }
}
