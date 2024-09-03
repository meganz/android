package mega.privacy.android.data.database

import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.user.UserCredentials

interface DatabaseHandler {

    //get the credential of last login
    val credentials: UserCredentials?

    @Deprecated("Call to MonitorEphemeralCredentialsUseCase instead")
    val ephemeral: EphemeralCredentials?

    /**
     * Gets preferences.
     *
     * @return Preferences.
     */
    val preferences: MegaPreferences?

    /**
     * Save chat settings in the current DB.
     *
     * @param chatSettings Chat settings to save.
     */
    var chatSettings: ChatSettings?

    /**
     * Sets the chat video quality value.
     * There are four possible values for this setting: VIDEO_QUALITY_ORIGINAL, VIDEO_QUALITY_HIGH,
     * VIDEO_QUALITY_MEDIUM or VIDEO_QUALITY_LOW.
     *
     * @param chatVideoQuality The new chat video quality.
     */
    var chatVideoQuality: Int

    var isPasscodeLockEnabled: Boolean
    var passcodeLockCode: String

    /**
     * Sets if the fingerprint lock setting is enabled or not.
     *
     * @param enabled True if the fingerprint is enabled, false otherwise.
     */
    var isFingerprintLockEnabled: Boolean

    /**
     * Sets the flag to indicate if should ask the user about set the current path as default download location.
     *
     * @param askSetDownloadLocation true if should ask, false otherwise.
     */
    var askSetDownloadLocation: Boolean
    val useHttpsOnly: String?

    /**
     * Flag to determine whether user should be shown a copyright page
     */
    val shouldShowCopyright: Boolean

    /**
     * Set the last public handle type value into the database.
     *
     * @param lastPublicHandleType Last public handle type value.
     */
    var lastPublicHandleType: Int

    /**
     * Set the handle of "My chat files" folder into the database.
     *
     * @param myChatFilesFolderHandle Handle value.
     */
    var myChatFilesFolderHandle: Long

    /**
     * Set the status of the transfer queue.
     *
     * @param transferQueueStatus True if the queue is paused, false otherwise.
     */
    var transferQueueStatus: Boolean
    val showNotifOff: String?
    val autoPlayEnabled: String?
    var sdCardUri: String?

    /**
     * Saves attributes in DB.
     *
     * @param attr Attributes to save.
     */
    var attributes: MegaAttributes?

    fun setNotificationSoundChat(sound: String?)
    fun setVibrationEnabledChat(enabled: String?)
    fun setNonContactFirstName(name: String?, handle: String?): Int
    fun setNonContactLastName(lastName: String?, handle: String?): Int
    fun setNonContactEmail(email: String?, handle: String?): Int

    @Deprecated("Should be removed when OfflineUtils is removed")
    fun exists(handle: Long): Boolean
    fun setFirstTime(firstTime: Boolean)
    fun setPreferredSortCloud(order: String?)
    fun setPreferredSortCameraUpload(order: String?)
    fun setPreferredSortOthers(order: String?)
    fun setLastCloudFolder(folderHandle: String)
    fun setAccountDetailsTimeStamp()
    fun resetAccountDetailsTimeStamp()
    fun setExtendedAccountDetailsTimestamp()
    fun resetExtendedAccountDetailsTimestamp()
    fun setStorageAskAlways(storageAskAlways: Boolean)
    fun setStorageDownloadLocation(storageDownloadLocation: String?)
    fun setAttrAskSizeDownload(askSizeDownload: String?)
    fun setUseHttpsOnly(useHttpsOnly: Boolean)
    fun setShowCopyright(showCopyright: Boolean)
    fun setShowNotifOff(showNotifOff: Boolean)
    fun setLastPublicHandle(handle: Long)
    fun setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp: Long)
    fun setLastPublicHandleTimeStamp()
    fun setInvalidateSdkCache(invalidateSdkCache: Boolean)
    fun clearCredentials()

    @Deprecated("Call to ClearEphemeralCredentialsUseCase instead")
    fun clearEphemeral()
    fun clearPreferences()
    fun clearAttributes()
    fun clearContacts()
    fun clearNonContacts()
    fun clearChatItems()
    fun clearChatSettings()

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     */
    fun updatePendingMessageOnTransferStart(idMessage: Long, transferTag: Int)

    /**
     * Updates a pending message.
     *
     * @param idMessage  Identifier of the pending message.
     * @param nodeHandle Handle of the node already uploaded.
     * @param state      State of the pending message.
     */
    fun updatePendingMessageOnTransferFinish(idMessage: Long, nodeHandle: String?, state: Int)

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     * @param nodeHandle  Handle of the node already uploaded.
     * @param state       State of the pending message.
     */
    fun updatePendingMessage(idMessage: Long, transferTag: Int, nodeHandle: String?, state: Int)
    fun removeSentPendingMessages()
    fun removePendingMessageByChatId(idChat: Long)
    fun setAutoPlayEnabled(enabled: String)
    fun findNonContactByHandle(handle: String): NonContactInfo?
    fun findContactByHandle(handleParam: Long): Contact?
    fun findContactByEmail(mail: String?): Contact?
}
