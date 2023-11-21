package mega.privacy.android.feature.sync.ui.permissions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ActivityContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Permissions manager for Sync feature
 */
class SyncPermissionsManager @Inject constructor(@ActivityContext private val context: Context) {

    // We temporary don't ask this permission
    /**
     * Checks if battery optimisation is disabled
     */
    fun isDisableBatteryOptimizationGranted(): Boolean = true

    /**
     * Checks if external storage permission is granted
     */
    fun isManageExternalStoragePermissionGranted(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Get manage external storage permission intent
     */
    @SuppressLint("InlinedApi")
    fun getManageExternalStoragePermissionIntent() =
        Intent(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION else Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addCategory("android.intent.category.DEFAULT")
            .setData(Uri.parse("package:${context.packageName}"))

    /**
     * Get disable batter optimisation  intent
     */
    @SuppressLint("BatteryLife")
    fun getDisableBatteryOptimizationsIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))

    /**
     * Get learn more page intent
     */
    fun getLearnMorePageIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.tac.vic.gov.au/__data/assets/pdf_file/0006/621456/How-to-disable-the-battery-optimization-on-Android-ammended-for-web-DPY-002.pdf")
        )

    /**
     * When android SDK is 11 or above
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isSDKAboveOrEqualToR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Launch all files/storage permission from app settings
     */
    @SuppressLint("InlinedApi")
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
    @SuppressLint("InlinedApi")
    fun launchAppSettingBatteryOptimisation() {
        val intent = getDisableBatteryOptimizationsIntent()
        runCatching {
            context.startActivity(intent)
        }.getOrElse {
            Timber.e(it)
        }
    }
}

