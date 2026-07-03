package mega.privacy.android.feature.photos.presentation.timeline

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build

/**
 * The runtime permissions required to enable Camera Uploads, resolved for the running OS version.
 *
 * These are hand-rolled here rather than reusing the app module's `PermissionUtils` because feature
 * modules cannot depend on `:app`, and the permission launcher needs the raw Manifest strings.
 */
internal fun getCameraUploadsPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            getImagePermissionByVersion(),
            getVideoPermissionByVersion(),
            READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else {
        arrayOf(getImagePermissionByVersion(), getVideoPermissionByVersion())
    }

internal fun getImagePermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) READ_MEDIA_IMAGES else READ_EXTERNAL_STORAGE

internal fun getVideoPermissionByVersion() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) READ_MEDIA_VIDEO else READ_EXTERNAL_STORAGE
