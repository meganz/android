package mega.privacy.android.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus

/**
 * A scope function similar to "let" to be used with compose blocks.
 * Useful to create composable functions if [this] is not null or null (instead of empty composable) if it is null
 */
inline fun <T> T.composeLet(crossinline block: @Composable (T) -> Unit): @Composable () -> Unit {
    return {
        block(this)
    }
}

/**
 * Wrapper for [com.google.accompanist.permissions.rememberPermissionState] to grant permissions when in Preview mode
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberPermissionState(
    permission: String,
    onPermissionResult: (Boolean) -> Unit = {},
): PermissionState =
    if (LocalInspectionMode.current) {
        object : PermissionState {
            override val permission: String = permission
            override val status: PermissionStatus = PermissionStatus.Granted
            override fun launchPermissionRequest() {}
        }
    } else {
        com.google.accompanist.permissions.rememberPermissionState(permission, onPermissionResult)
    }
