package mega.privacy.android.navigation

import android.app.Activity

/**
 * App module navigator
 *
 */
interface AppNavigator {
    /**
     * Open setting camera upload
     *
     * @param activity
     */
    fun openSettingCameraUpload(activity: Activity)

    /**
     * Open backup
     *
     * @param activity
     */
    fun openBackup(activity: Activity)

    /**
     * Open file cloud drive
     *
     * @param activity
     */
    fun openFileCloudDrive(activity: Activity)
}