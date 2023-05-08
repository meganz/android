package mega.privacy.android.feature.sync.ui.navigator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
import javax.inject.Inject

internal class SyncNavigatorImpl @Inject constructor() : SyncNavigator {

    override fun startSyncService(activity: Activity) {
        // Note: the version that being merged to develop does not have the permissions
        // defined in the Manifest. Therefore the requestPermissions method is commented out
//        requestPermissions(activity)
        startSyncBackgroundService(activity)
    }

    private fun requestPermissions(activity: Activity) {
        requestManageExternalStoragePermission(activity)
        turnOffBatteryOptimizations(activity)
    }

    private fun requestManageExternalStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
        }
    }

    private fun turnOffBatteryOptimizations(activity: Activity) {
        val intent = Intent()
        val powerManager =
            activity.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivity(intent)
        }
    }

    private fun startSyncBackgroundService(context: Context) {
        SyncBackgroundService.start(context)
    }
}
