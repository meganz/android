package mega.privacy.android.app.nav

import android.app.Activity
import android.content.Intent
import mega.privacy.android.app.activities.settingsActivities.CameraUploadsPreferencesActivity
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.navigation.AppNavigator

internal interface AppNavigatorImpl : AppNavigator {
    override fun openSettingsCameraUploads(activity: Activity) {
        activity.startActivity(
            Intent(
                activity,
                CameraUploadsPreferencesActivity::class.java
            )
        )
    }

    override fun openBackup(activity: Activity) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(DrawerItem.BACKUPS)
        }
    }

    override fun openFileCloudDrive(activity: Activity) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(DrawerItem.CLOUD_DRIVE)
        }
    }
}