package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen
import mega.privacy.android.app.presentation.permissions.model.PermissionType

/**
 * Gets the [PermissionType] depending on the current [PermissionScreen] and the missing permissions.
 *
 * @param missingPermissions Missing permission pending to request.
 * @return The [PermissionType].
 */
fun PermissionScreen.toPermissionType(missingPermissions: List<Permission>): PermissionType =
    when (this) {
        PermissionScreen.Media -> when {
            missingPermissions.contains(Permission.Read)
                    && missingPermissions.contains(Permission.Write) -> PermissionType.ReadAndWrite
            missingPermissions.contains(Permission.Write) -> PermissionType.Write
            else -> PermissionType.Read
        }
        PermissionScreen.Camera -> PermissionType.Camera
        PermissionScreen.Calls -> when {
            missingPermissions.contains(Permission.Microphone)
                    && missingPermissions.contains(Permission.Bluetooth) -> PermissionType.MicrophoneAndBluetooth
            missingPermissions.contains(Permission.Microphone) -> PermissionType.Microphone
            else -> PermissionType.Bluetooth
        }
        PermissionScreen.Contacts -> PermissionType.Contacts
        else -> PermissionType.Notifications
    }