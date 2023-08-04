package mega.privacy.android.data.repository

import android.Manifest
import android.os.Build
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.domain.repository.PermissionRepository
import timber.log.Timber
import javax.inject.Inject

internal class PermissionRepositoryImpl @Inject constructor(
    private val permissionGateway: PermissionGateway
) : PermissionRepository {

    override fun hasMediaPermission(): Boolean {
        val hasMediaPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ) || permissionGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                permissionGateway.hasPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            }
        return hasMediaPermissions.also {
            Timber.d("Device has required permissions $it")
        }
    }
}