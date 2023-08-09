package mega.privacy.android.app.presentation.extensions

import android.os.Build
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen

internal val PermissionScreen.title: Int
    get() = when (this) {
        PermissionScreen.Notifications -> R.string.permissions_notifications_title
        PermissionScreen.Media -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            R.string.allow_access_audio_title
        } else {
            R.string.allow_acces_media_title
        }

        PermissionScreen.Camera -> R.string.allow_acces_camera_title
        PermissionScreen.Calls -> R.string.allow_acces_calls_title
        PermissionScreen.Contacts -> R.string.allow_acces_contact_title
    }

internal val PermissionScreen.description: Int
    get() = when (this) {
        PermissionScreen.Notifications -> R.string.permissions_notifications_description
        PermissionScreen.Media -> R.string.allow_acces_media_subtitle
        PermissionScreen.Camera -> R.string.allow_acces_camera_subtitle
        PermissionScreen.Calls -> R.string.allow_acces_calls_subtitle_microphone
        PermissionScreen.Contacts -> R.string.allow_acces_contact_subtitle
    }

internal val PermissionScreen.image: Int
    get() = when (this) {
        PermissionScreen.Notifications -> R.drawable.ic_notifications_permission
        PermissionScreen.Media -> R.drawable.photos
        PermissionScreen.Camera -> R.drawable.enable_camera
        PermissionScreen.Calls -> R.drawable.calls
        PermissionScreen.Contacts -> R.drawable.contacts
    }