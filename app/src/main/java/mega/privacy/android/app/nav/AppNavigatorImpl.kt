package mega.privacy.android.app.nav

import android.app.Activity
import androidx.annotation.StringRes
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.navigation.AppNavigator

internal interface AppNavigatorImpl : AppNavigator {

    override fun openNodeInBackups(
        activity: Activity,
        backupsHandle: Long,
        @StringRes errorMessage: Int?,
    ) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(
                item = DrawerItem.BACKUPS,
                backupsHandle = backupsHandle,
                errorMessage = errorMessage,
            )
        }
    }

    override fun openNodeInCloudDrive(
        activity: Activity,
        nodeHandle: Long,
        @StringRes errorMessage: Int?,
    ) {
        if (activity is ManagerActivity) {
            activity.selectDrawerItem(
                item = DrawerItem.CLOUD_DRIVE,
                cloudDriveNodeHandle = nodeHandle,
                errorMessage = errorMessage,
            )
        }
    }
}