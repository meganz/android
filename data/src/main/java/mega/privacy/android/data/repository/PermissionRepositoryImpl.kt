package mega.privacy.android.data.repository

import android.Manifest
import android.os.Build
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.domain.repository.PermissionRepository
import timber.log.Timber
import javax.inject.Inject

internal class PermissionRepositoryImpl @Inject constructor(
    private val permissionGateway: PermissionGateway,
) : PermissionRepository {

    override fun hasMediaPermission(): Boolean {
        val hasMediaPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionGateway.hasPermissions(
                    permissionGateway.getImagePermissionByVersion(),
                    permissionGateway.getVideoPermissionByVersion()
                ) || permissionGateway.hasPermissions(permissionGateway.getPartialMediaPermission())
            } else {
                permissionGateway.hasPermissions(
                    permissionGateway.getImagePermissionByVersion(),
                    permissionGateway.getVideoPermissionByVersion()
                )
            }
        return hasMediaPermissions.also {
            Timber.d("Device has required permissions $it")
        }
    }

    override fun hasAudioPermission(): Boolean =
        permissionGateway.hasPermissions(permissionGateway.getAudioPermissionByVersion())
}