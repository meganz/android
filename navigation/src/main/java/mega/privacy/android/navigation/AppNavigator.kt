package mega.privacy.android.navigation

import android.app.Activity
import android.content.Context

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
        chatId: Long? = null,
        action: String? = null,
        link: String? = null,
        text: String? = null,
        messageId: Long? = null,
        isOverQuota: Int? = null,
        flags: Int = 0
    )
}