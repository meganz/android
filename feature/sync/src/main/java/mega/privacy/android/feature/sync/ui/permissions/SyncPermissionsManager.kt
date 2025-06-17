package mega.privacy.android.feature.sync.ui.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Permissions manager for Sync feature
 */
class SyncPermissionsManager @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Checks if battery optimisation is disabled
     *
     * This permission allows us to run background service without being killed by system
     * even when the app is removed from recent apps
     *
     * Note that for Android 14 we are running a foreground service, therefore we don't need to
     * request disable battery optimization permission on Android 14, therefore it's defaulted to `true`
     */
    fun isDisableBatteryOptimizationGranted(): Boolean {
        val powerManager =
            context.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Checks if external storage permission is granted
     */
    fun isManageExternalStoragePermissionGranted(): Boolean = if (isSDKAboveOrEqualToR())
        true
    else
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Get manage external storage permission intent
     */
    private fun getManageExternalStoragePermissionIntent() =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )
            .addCategory("android.intent.category.DEFAULT")
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Get disable batter optimisation intent
     */
    @SuppressLint("BatteryLife")
    fun getDisableBatteryOptimizationsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Check whether the permission to send notifications is granted
     */
    fun isNotificationsPermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }


    /**
     * When android SDK is 11 or above
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isSDKAboveOrEqualToR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * When android SDK is 14 or above
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun isSDKAboveOrEqualTo14() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /**
     * Launch all files/storage permission from app settings
     */
    fun launchAppSettingFileStorageAccess() {
        val intent = getManageExternalStoragePermissionIntent()
        runCatching {
            context.startActivity(intent)
        }.getOrElse {
            Timber.e(it)
        }
    }

    /**
     * Launch all files/storage permission from app settings
     */
    fun launchAppSettingBatteryOptimisation() {
        val intent = getDisableBatteryOptimizationsIntent()
        runCatching {
            context.startActivity(intent)
        }.getOrElse {
            Timber.e(it)
        }
    }
}
