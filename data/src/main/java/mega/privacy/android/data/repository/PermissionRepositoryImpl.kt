package mega.privacy.android.data.repository

import android.Manifest
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.PermissionRepository
import timber.log.Timber
import javax.inject.Inject

internal class PermissionRepositoryImpl @Inject constructor(
    private val permissionGateway: PermissionGateway,
    private val uiPreferencesGateway: UIPreferencesGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

    override fun hasManageExternalStoragePermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            permissionGateway.hasPermissions(permissionGateway.getReadExternalStoragePermission())
        }

    override fun isLocationPermissionGranted(): Boolean {
        return permissionGateway.hasPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION)
                || permissionGateway.hasPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun hasNotificationPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                permissionGateway.hasPermissions(Manifest.permission.POST_NOTIFICATIONS)

    override suspend fun setNotificationPermissionShownTimestamp(timestamp: Long) =
        withContext(ioDispatcher) {
            uiPreferencesGateway.setNotificationPermissionShownTimestamp(timestamp)
        }

    override suspend fun monitorNotificationPermissionShownTimestamp() =
        uiPreferencesGateway
            .monitorNotificationPermissionShownTimestamp()
            .flowOn(ioDispatcher)

    override fun hasCameraUploadsPermission(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                permissionGateway.getImagePermissionByVersion(),
                permissionGateway.getVideoPermissionByVersion(),
                permissionGateway.getPartialMediaPermission(),
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                permissionGateway.getImagePermissionByVersion(),
                permissionGateway.getVideoPermissionByVersion()
            )
        } else {
            arrayOf(
                permissionGateway.getImagePermissionByVersion(),
                permissionGateway.getVideoPermissionByVersion()
            )
        }
        val hasMediaPermissions = permissionGateway.hasPermissions(*permissions)
        return hasMediaPermissions.also {
            Timber.d("Device has required CU permissions $it")
        }
    }
}