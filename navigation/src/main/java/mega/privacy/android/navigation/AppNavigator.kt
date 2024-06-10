package mega.privacy.android.navigation

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes

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
     * @param errorMessage The [StringRes] of the message to display in the error banner
     */
    fun openNodeInBackups(activity: Activity, backupsHandle: Long, @StringRes errorMessage: Int?)

    /**
     * Navigates to the Cloud Drive page to view the selected Node
     *
     * @param activity the Activity
     * @param nodeHandle The Node Handle to view the selected Node. The Root Node will be accessed
     * if no Node Handle is specified
     * @param errorMessage The [StringRes] of the message to display in the error banner
     */
    fun openNodeInCloudDrive(
        activity: Activity,
        nodeHandle: Long = -1L,
        @StringRes errorMessage: Int?,
    )

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
     * Navigates to the new [mega.privacy.android.app.presentation.meeting.managechathistory.view.screen.ManageChatHistoryActivityV2]
     *
     * @param context The context that call this method
     * @param chatId The chat ID of the chat or meeting room
     * @param email The email of the current user
     */
    fun openManageChatHistoryActivity(
        context: Context,
        chatId: Long = -1L,
        email: String? = null,
    )

    /**
     * Open upgrade account screen.
     * This screen allows users to upgrade to a paid plan
     */
    fun openUpgradeAccount(context: Context)

    /**
     * Navigates to the Syncs page
     *
     * @param activity      The Activity
     * @param deviceName    The device name
     * @param openNewSync   True to directly open New Sync screen, False otherwise.
     */
    fun openSyncs(activity: Activity, deviceName: String? = null, openNewSync: Boolean = false)

    /**
     * Open zip browser
     *
     * @param context Context
     * @param zipFilePath zip file path
     * @param nodeHandle the node handle of zip file
     * @param onError Callback called when zip file format check is not passed
     */
    fun openZipBrowserActivity(
        context: Context,
        zipFilePath: String,
        nodeHandle: Long? = null,
        onError: () -> Unit,
    )
}
