package mega.privacy.android.data.repository

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

    override suspend fun setNotificationPermissionShownTimestamp(timestamp: Long) =
        withContext(ioDispatcher) {
            uiPreferencesGateway.setNotificationPermissionShownTimestamp(timestamp)
        }

    override suspend fun monitorNotificationPermissionShownTimestamp() =
        uiPreferencesGateway
            .monitorNotificationPermissionShownTimestamp()
            .flowOn(ioDispatcher)
}