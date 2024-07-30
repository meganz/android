package mega.privacy.android.data.gateway

import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.settings.ChatSettings

/**
 * Mega local storage gateway
 *
 * @constructor Create empty Mega local storage gateway
 */
interface MegaLocalStorageGateway {

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): Int

    /**
     * Get links sort order
     * @return links sort order
     */
    suspend fun getLinksSortOrder(): Int

    /**
     * Get offline sort order
     * @return offline sort order
     */
    suspend fun getOfflineSortOrder(): Int

    /**
     * Set offline sort order
     * @param order
     */
    suspend fun setOfflineSortOrder(order: Int)

    /**
     * Set cloud sort order
     * @param order
     */
    suspend fun setCloudSortOrder(order: Int)

    /**
     * Set camera sort order
     * @param order
     */
    suspend fun setCameraSortOrder(order: Int)

    /**
     * Set others sort order
     * @param order
     */
    suspend fun setOthersSortOrder(order: Int)

    /**
     * Do user preferences exist
     */
    suspend fun doPreferencesExist(): Boolean

    /**
     * Get non contact by handle
     *
     * @param userHandle
     */
    suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo?

    /**
     * Set non contact email
     *
     * @param userHandle
     * @param email
     */
    suspend fun setNonContactEmail(userHandle: Long, email: String)


    /**
     * Get contact by email
     *
     * @param email
     * @return local contact details if found
     */
    suspend fun getContactByEmail(email: String?): Contact?

    /**
     * Set first time
     *
     */
    suspend fun setUserHasLoggedIn()

    /**
     * Get storage download location
     *
     * @return storageDownloadLocation path as [String]
     */
    suspend fun getStorageDownloadLocation(): String?

    /**
     * Get always ask for storage download location value
     *
     * @return isStorageAskAlways as [Boolean]
     */
    suspend fun isStorageAskAlways(): Boolean

    /**
     * Set to always ask for storage download location
     *
     * @param isStorageAskAlways
     */
    suspend fun setStorageAskAlways(isStorageAskAlways: Boolean)

    /**
     * @return if should ask the user about download location
     */
    suspend fun isAskSetDownloadLocation(): Boolean

    /**
     * Set if should ask the user about download location
     */
    suspend fun setAskSetDownloadLocation(value: Boolean)

    /**
     * Set storage download location
     *
     * @param storageDownloadLocation
     */
    suspend fun setStorageDownloadLocation(storageDownloadLocation: String)

    /**
     * @return ask for confirmation before large downloads preference
     */
    suspend fun isAskBeforeLargeDownloads(): Boolean

    /**
     * Set ask for confirmation before large downloads preference
     */
    suspend fun setAskBeforeLargeDownloads(askForConfirmation: Boolean)

    /**
     * Set passcode l ock enabled
     *
     * @param isPasscodeLockEnabled
     */
    fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean)

    /**
     * Set the passcode lock code
     *
     * @param passcodeLockCode
     */
    suspend fun setPasscodeLockCode(passcodeLockCode: String)

    /**
     * Set show copyright
     *
     * @param showCopyrights
     */
    suspend fun setShowCopyright(showCopyrights: Boolean)

    /**
     * Gets attributes from DB
     */
    suspend fun getAttributes(): MegaAttributes?

    /**
     * Get chat files folder handle
     */
    suspend fun getChatFilesFolderHandle(): Long?

    /**
     * Set last public handle
     *
     * @param handle
     */
    suspend fun setLastPublicHandle(handle: Long)

    /**
     * Set last public handle time stamp
     */
    suspend fun setLastPublicHandleTimeStamp()

    /**
     * Set last public handle type
     *
     * @param type
     */
    suspend fun setLastPublicHandleType(type: Int)

    /**
     * Gets chat settings.
     */
    suspend fun getChatSettings(): ChatSettings?

    /**
     * Sets chat settings.
     *
     * @param chatSettings [ChatSettings]
     */
    suspend fun setChatSettings(chatSettings: ChatSettings)

    /**
     * Clear preferences
     */
    suspend fun clearPreferences()

    /**
     * Sets first time.
     */
    suspend fun setFirstTime(firstTime: Boolean)

    /**
     * Get first time
     */
    suspend fun getFirstTime(): Boolean?

    /**
     * Clears contacts.
     */
    suspend fun clearContacts()

    /**
     * Clears non contacts.
     */
    suspend fun clearNonContacts()

    /**
     * Clears chat items.
     */
    suspend fun clearChatItems()

    /**
     * clears attributes.
     */
    suspend fun clearAttributes()

    /**
     * Clears chat settings.
     */
    suspend fun clearChatSettings()

    /**
     * Gets pending messages.
     *
     * @param chatId Chat identifier from which the messages has to be get.
     * @return A list of [AndroidMegaChatMessage].
     */
    suspend fun findPendingMessagesNotSent(chatId: Long): List<AndroidMegaChatMessage>

    /**
     * Set transfer queue status
     *
     * @param isPause
     */
    suspend fun setTransferQueueStatus(isPause: Boolean)

    /**
     * Get transfer queue status
     *
     * @return true if is paused, false otherwise
     */
    suspend fun getTransferQueueStatus(): Boolean

    /**
     * Remove pending message by chat id
     *
     * @param chatId Chat id.
     */
    fun removePendingMessageByChatId(chatId: Long)

    /**
     * Should show copyright
     */
    fun shouldShowCopyright(): Boolean
}
