package mega.privacy.android.feature.sync.ui.permissions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Permissions manager for Sync feature
 */
internal class SyncPermissionsManager {

    // We temporary don't ask this permission
    fun isDisableBatteryOptimizationGranted(context: Context): Boolean = true

    fun isManageExternalStoragePermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("InlinedApi")
    fun getManageExternalStoragePermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).addCategory("android.intent.category.DEFAULT")
            .setData(Uri.parse("package:${context.packageName}"))

    @SuppressLint("BatteryLife")
    fun getDisableBatteryOptimizationsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))

    fun getLearnMorePageIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.tac.vic.gov.au/__data/assets/pdf_file/0006/621456/How-to-disable-the-battery-optimization-on-Android-ammended-for-web-DPY-002.pdf")
        )
}

