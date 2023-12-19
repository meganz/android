package mega.privacy.android.data.database

import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.user.UserCredentials

interface DatabaseHandler {

    val myEmail: String?

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

    /**
     * Sets the flag to indicate if the local path selected as Media Uploads local folder belongs to an external SD card.
     *
     * @param mediaFolderExternalSdCard true if the local path selected belongs to an external SD card, false otherwise
     */
    var passcodeLockType: String?
    var isPasscodeLockEnabled: Boolean
    var passcodeLockCode: String

    /**
     * Sets the time required before ask for the passcode.
     *
     * @param requiredTime The time required before ask for the passcode.
     */
    var passcodeRequiredTime: Int

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
     * Set the storage state value into the database.
     *
     * @param storageState Storage state value.
     */
    var storageState: StorageState

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

    fun saveCredentials(userCredentials: UserCredentials)
    fun saveMyEmail(email: String?)
    fun saveMyFirstName(firstName: String?)
    fun saveMyLastName(lastName: String?)
    fun shouldAskForDisplayOver(): Boolean
    fun dontAskForDisplayOver()
    fun setNotificationSoundChat(sound: String?)
    fun setVibrationEnabledChat(enabled: String?)
    fun setWrittenTextItem(handle: String?, text: String?, editedMsgId: String?): Int
    fun areNotificationsEnabled(handle: String?): String?

    fun setNonContactFirstName(name: String?, handle: String?): Int
    fun setNonContactLastName(lastName: String?, handle: String?): Int
    fun setNonContactEmail(email: String?, handle: String?): Int
    fun setContactNickname(nickname: String?, handle: Long)

    fun exists(handle: Long): Boolean
    fun removeById(id: Int): Int
    fun setFirstTime(firstTime: Boolean)
    fun setCamSyncWifi(wifi: Boolean)
    fun setPreferredViewList(list: Boolean)
    fun setPreferredViewListCamera(list: Boolean)
    fun setPreferredSortCloud(order: String?)
    fun setPreferredSortCameraUpload(order: String?)
    fun setPreferredSortOthers(order: String?)
    fun setLastUploadFolder(folderPath: String)
    fun setLastCloudFolder(folderHandle: String)
    fun setKeepFileNames(charging: Boolean)
    fun setCamSyncEnabled(enabled: Boolean)
    fun setSecondaryUploadEnabled(enabled: Boolean)
    fun setCamSyncHandle(handle: Long)
    fun setSecondaryFolderHandle(handle: Long)
    fun setAccountDetailsTimeStamp()
    fun resetAccountDetailsTimeStamp()
    fun setExtendedAccountDetailsTimestamp()
    fun resetExtendedAccountDetailsTimestamp()
    fun setStorageAskAlways(storageAskAlways: Boolean)
    fun setStorageDownloadLocation(storageDownloadLocation: String?)
    fun setAttrAskSizeDownload(askSizeDownload: String?)
    fun setAttrAskNoAppDownload(askNoAppDownload: String?)
    fun setAttrAttempts(attempt: Int)
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
    fun clearOffline()

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
    fun updatePendingMessageOnAttach(idMessage: Long, temporalId: String?, state: Int)
    fun findPendingMessageByIdTempKarere(idTemp: Long): Long
    fun removeSentPendingMessages()
    fun removePendingMessageByChatId(idChat: Long)
    fun removePendingMessageById(idMsg: Long)
    fun setAutoPlayEnabled(enabled: String)
    fun setShowInviteBanner(show: String)
    fun findNonContactByHandle(handle: String): NonContactInfo?
    fun findContactByHandle(handleParam: Long): Contact?
    fun findContactByEmail(mail: String?): Contact?

    /**
     * Get list of [Offline]
     *
     * @param nodePath
     * @param searchQuery
     *
     * @return List of [Offline]
     */
    suspend fun getOfflineInformationList(
        nodePath: String,
        searchQuery: String?,
    ): List<Offline>

    /**
     * Gets pending messages.
     *
     * @param chatId Chat identifier from which the messages has to be get.
     * @return A list of [AndroidMegaChatMessage].
     */
    fun findPendingMessagesNotSent(chatId: Long): List<AndroidMegaChatMessage>
}
