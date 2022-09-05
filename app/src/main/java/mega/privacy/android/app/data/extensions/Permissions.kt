package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.app.presentation.permissions.model.Permission

/**
 * Removes the permissions already granted.
 *
 * @return The list with the missing permissions.
 */
fun List<Pair<Permission, Boolean>>.filterAllowedPermissions(): List<Permission> =
    filter { permission -> !permission.second }
        .map { (first) -> first }

/**
 * Gets a [PermissionScreen] mutable list from the [Permission] list.
 *
 * @return Mutable list of [PermissionScreen].
 */
fun List<Permission>.toPermissionScreen(): MutableList<PermissionScreen> =
    mutableListOf<PermissionScreen>().apply {
        this@toPermissionScreen.forEach { permissionType ->
            when (permissionType) {
                Permission.Notifications -> add(PermissionScreen.Notifications)
                Permission.Read -> add(PermissionScreen.Media)
                Permission.Write -> if (!contains(PermissionScreen.Media))
                    add(PermissionScreen.Media)
                Permission.Camera -> add(PermissionScreen.Camera)
                Permission.Microphone -> add(PermissionScreen.Calls)
                Permission.Bluetooth -> if (!contains(PermissionScreen.Calls))
                    add(PermissionScreen.Calls)
                Permission.Contacts -> add(PermissionScreen.Contacts)
            }
        }
    }

