package mega.privacy.android.navigation

import android.app.Activity
import android.content.Context
import androidx.annotation.DrawableRes

/**
 * App module navigator
 *
 */
interface AppNavigator {
    /**
     * Navigates to the Settings Camera Uploads page
     *
     * @param activity The Activity
     */
    fun openSettingsCameraUploads(activity: Activity)

    /**
     * Navigates to the Backups page to load the contents of the Backup Folder
     *
     * @param activity the Activity
     * @param backupsHandle The Backups Handle used to load its contents
     */
    fun openNodeInBackups(activity: Activity, backupsHandle: Long)

    /**
     * Navigates to the Cloud Drive page to view the selected Node
     *
     * @param activity the Activity
     * @param nodeHandle The Node Handle to view the selected Node. The Root Node will be accessed
     * if no Node Handle is specified
     */
    fun openNodeInCloudDrive(activity: Activity, nodeHandle: Long = -1L)

    /**
     * Open chat
     *
     * @param context
     * @param chatId chat id of the chat room
     * @param action action of the intent
     * @param link chat link
     * @param text text to show in snackbar
     * @param messageId message id
     * @param isOverQuota is over quota int value
     */
    fun openChat(
        context: Context,
        chatId: Long,
        action: String? = null,
        link: String? = null,
        text: String? = null,
        messageId: Long? = null,
        isOverQuota: Int? = null,
        flags: Int = 0,
    )

    /**
     * Shows the legacy Bottom Sheet with specific Options depending on the type of Device Center
     * Folder
     *
     * @param activity the Activity
     * @param isBackupsFolder true if this is a Backups Folder, and false if otherwise
     * @param nodeName the Node Name
     * @param nodeHandle the Node Handle
     * @param nodeStatus the Node Status
     * @param nodeStatusColorInt an optional Text Color for the Node Status, represented as an [Int]
     * @param nodeIcon the Node Icon as an [Int]
     * @param nodeStatusIcon an optional Icon for the Node Status, represented as an [Int]
     */
    fun openDeviceCenterFolderNodeOptions(
        activity: Activity,
        isBackupsFolder: Boolean,
        nodeName: String,
        nodeHandle: Long,
        nodeStatus: String,
        nodeStatusColorInt: Int? = null,
        @DrawableRes nodeIcon: Int,
        @DrawableRes nodeStatusIcon: Int? = null,
    )
}