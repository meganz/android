package mega.privacy.android.app.appstate.content.navigation.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.permissions.PermissionsScreenComposable

@Serializable
data class PermissionScreensNavKey(
    val onlyShowNotificationPermission: Boolean = false,
) : NavKey

@Composable
fun PermissionScreens(
    onlyShowNotificationPermission: Boolean,
    onPermissionsCompleted: (isCameraUploadsEnabled: Boolean) -> Unit,
    activity: ComponentActivity,
) {
    PermissionsScreenComposable(
        onPermissionsCompleted = onPermissionsCompleted,
        activity = activity,
        onlyShowNotificationPermission = onlyShowNotificationPermission,
    )
}
