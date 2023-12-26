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

    override fun openNodeInBackups(activity: Activity, backupsHandle: Long) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(
                item = DrawerItem.BACKUPS,
                backupsHandle = backupsHandle,
                alwaysInitializeDrawerItem = true,
            )
        }
    }

    override fun openNodeInCloudDrive(activity: Activity, nodeHandle: Long) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(
                item = DrawerItem.CLOUD_DRIVE,
                cloudDriveNodeHandle = nodeHandle,
            )
        }
    }
}