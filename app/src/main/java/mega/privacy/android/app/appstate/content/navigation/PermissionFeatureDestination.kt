package mega.privacy.android.app.appstate.content.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreens
import mega.privacy.android.app.appstate.content.navigation.view.PermissionScreensNavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

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
        PermissionScreens(
            onlyShowNotificationPermission = it.onlyShowNotificationPermission,
            onPermissionsCompleted = { _ ->
                navigationHandler.back()
            },
            activity = activity ?: return@entry,
        )
    }
}
