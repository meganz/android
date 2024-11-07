package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import android.os.Build
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.permissions.model.PermissionScreen

internal val PermissionScreen.title: Int
    get() = when (this) {
        PermissionScreen.Notifications -> R.string.permissions_notifications_title
        PermissionScreen.DisplayOverOtherApps -> sharedR.string.display_over_other_apps_permission_screen_title
        PermissionScreen.Media -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            R.string.allow_access_audio_title
        } else {
            R.string.allow_acces_media_title
        }

        PermissionScreen.Camera -> R.string.allow_acces_camera_title
        PermissionScreen.Calls -> R.string.allow_acces_calls_title
    }

internal val PermissionScreen.description: Int
    get() = when (this) {
        PermissionScreen.Notifications -> R.string.permissions_notifications_description
        PermissionScreen.DisplayOverOtherApps -> sharedR.string.display_over_other_apps_permission_screen_message
        PermissionScreen.Media -> R.string.allow_acces_media_subtitle
        PermissionScreen.Camera -> R.string.allow_acces_camera_subtitle
        PermissionScreen.Calls -> R.string.allow_acces_calls_subtitle_microphone
    }

internal val PermissionScreen.image: Int
    get() = when (this) {
        PermissionScreen.Notifications -> iconPackR.drawable.ic_bell_glass
        PermissionScreen.DisplayOverOtherApps -> iconPackR.drawable.ic_phone_glass
        PermissionScreen.Media -> iconPackR.drawable.ic_image_glass
        PermissionScreen.Camera -> iconPackR.drawable.ic_video_glass
        PermissionScreen.Calls -> iconPackR.drawable.ic_message_call_glass
    }

internal val PermissionScreen.positiveButton: Int
    get() = when (this) {
        PermissionScreen.DisplayOverOtherApps -> sharedR.string.general_allow_permission_positive_button
        else -> R.string.button_continue
    }