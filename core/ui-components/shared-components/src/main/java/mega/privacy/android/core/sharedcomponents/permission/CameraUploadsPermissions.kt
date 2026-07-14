package mega.privacy.android.core.sharedcomponents.permission

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build

/**
 * The runtime media permissions required to enable Camera uploads, resolved for the running OS
 * version. Returns the raw [android.Manifest.permission] strings so they can be passed straight to
 * a permission launcher.
 */
fun getCameraUploadsPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            getImagePermissionByVersion(),
            getVideoPermissionByVersion(),
            READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else {
        arrayOf(
            getImagePermissionByVersion(),
            getVideoPermissionByVersion()
        )
    }

internal fun getImagePermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) READ_MEDIA_IMAGES else READ_EXTERNAL_STORAGE

internal fun getVideoPermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) READ_MEDIA_VIDEO else READ_EXTERNAL_STORAGE
