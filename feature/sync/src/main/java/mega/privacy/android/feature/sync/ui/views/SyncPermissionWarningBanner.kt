package mega.privacy.android.feature.sync.ui.views

import android.Manifest
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import mega.privacy.android.core.ui.controls.banners.WarningBanner
import mega.privacy.android.core.ui.utils.ComposableLifecycle
import mega.privacy.android.core.ui.utils.rememberPermissionState
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

/**
 * Permission banner shown on top of sync screens
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SyncPermissionWarningBanner(
    syncPermissionsManager: SyncPermissionsManager,
) {
    val storagePermission =
        rememberPermissionState(permission = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    var allFileAccess by remember { mutableStateOf(false) }
    var hasUnrestrictedBatteryUsage by remember { mutableStateOf(false) }

    ComposableLifecycle { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            allFileAccess = if (syncPermissionsManager.isSDKAboveOrEqualToR()) {
                Environment.isExternalStorageManager()
            } else {
                storagePermission.status.isGranted
            }
            hasUnrestrictedBatteryUsage =
                syncPermissionsManager.isDisableBatteryOptimizationGranted()
        }
    }
    if (allFileAccess.not()) {
        WarningBanner(
            textString = stringResource(id = R.string.sync_storage_permission_banner),
            onCloseClick = null,
            modifier = Modifier.clickable {
                if (syncPermissionsManager.isSDKAboveOrEqualToR()) {
                    syncPermissionsManager.launchAppSettingFileStorageAccess()
                } else {
                    if (storagePermission.status.isGranted.not() && storagePermission.status.shouldShowRationale.not()) {
                        syncPermissionsManager.launchAppSettingFileStorageAccess()
                    } else {
                        storagePermission.launchPermissionRequest()
                    }
                }
            }
        )
    }
    if (allFileAccess && hasUnrestrictedBatteryUsage.not()) {
        WarningBanner(
            textString = stringResource(id = R.string.sync_battery_optimisation_banner),
            onCloseClick = null,
            modifier = Modifier.clickable {
                syncPermissionsManager.launchAppSettingBatteryOptimisation()
            },
        )
    }
}