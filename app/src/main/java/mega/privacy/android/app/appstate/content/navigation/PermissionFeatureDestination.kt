package mega.privacy.android.app.appstate.content.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreens
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.shared.resources.R as sharedR

class PermissionFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            permissionScreensDestination(
                navigationHandler = navigationHandler
            )
        }
}

fun EntryProviderScope<NavKey>.permissionScreensDestination(
    navigationHandler: NavigationHandler,
) {
    entry<PermissionScreensNavKey> {
        val activity = LocalActivity.current as? ComponentActivity
        val context = LocalContext.current
        val snackbarHostState = LocalSnackBarHostState.current
        var pendingCameraUploadsEnabled by remember { mutableStateOf(false) }

        PermissionScreens(
            onlyShowNotificationPermission = it.onlyShowNotificationPermission,
            onPermissionsCompleted = { isCameraUploadsEnabled ->
                if (isCameraUploadsEnabled) {
                    pendingCameraUploadsEnabled = true
                } else {
                    navigationHandler.back()
                }
            },
            activity = activity ?: return@entry,
        )

        LaunchedEffect(pendingCameraUploadsEnabled) {
            if (pendingCameraUploadsEnabled) {
                //TODO AND-21782 navigate to MEDIA Tab
                navigationHandler.back()
                snackbarHostState?.showAutoDurationSnackbar(context.getString(sharedR.string.onboarding_camera_upload_permission_enabled_message))
            }
        }
    }
}
