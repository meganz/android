package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.shared.original.core.ui.controls.banners.WarningBanner
import java.util.concurrent.TimeUnit

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
    val coroutineScope = rememberCoroutineScope()

    LifecycleResumeEffect(syncPermissionsManager) {
        val job = coroutineScope.launch {
            delay(TimeUnit.SECONDS.toMillis(2))
            // this delay is used to ensure that other top priority warnings are shown first
            hasUnrestrictedBatteryUsage =
                syncPermissionsManager.isDisableBatteryOptimizationGranted()
        }
        onPauseOrDispose {
            job.cancel()
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
