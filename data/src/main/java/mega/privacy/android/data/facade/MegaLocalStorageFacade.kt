package mega.privacy.android.data.facade

import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.entity.user.UserCredentials
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_FAV_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LABEL_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import javax.inject.Inject

/**
 * Mega preferences facade
 *
 * Implements [MegaLocalStorageGateway] and provides a facade over [DatabaseHandler]
 *
 * @property dbHandler
 */
internal class MegaLocalStorageFacade @Inject constructor(
    private val dbHandler: DatabaseHandler,
) : MegaLocalStorageGateway {

    override suspend fun getCloudSortOrder(): Int =
        dbHandler.preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getCameraSortOrder(): Int =
        dbHandler.preferences?.preferredSortCameraUpload?.toInt() ?: ORDER_MODIFICATION_DESC

    override suspend fun getOthersSortOrder(): Int =
        dbHandler.preferences?.preferredSortOthers?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getLinksSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_MODIFICATION_ASC -> ORDER_LINK_CREATION_ASC
            ORDER_MODIFICATION_DESC -> ORDER_LINK_CREATION_DESC
            else -> order
        }

    /**
     * Since offline nodes cannot be ordered by labels and favorites, the offline order will be same as
     * cloud order except when cloud order is ORDER_LABEL_ASC or ORDER_FAV_ASC where it defaults to
     * ORDER_DEFAULT_ASC.
     */
    override suspend fun getOfflineSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_LABEL_ASC -> ORDER_DEFAULT_ASC
            ORDER_FAV_ASC -> ORDER_DEFAULT_ASC
            else -> order
        }

    override suspend fun setOfflineSortOrder(order: Int) {
        dbHandler.setPreferredSortCloud(order.toString())
    }

    override suspend fun setCloudSortOrder(order: Int) {
        dbHandler.setPreferredSortCloud(order.toString())
    }

    override suspend fun setCameraSortOrder(order: Int) {
        dbHandler.setPreferredSortCameraUpload(order.toString())
    }

    override suspend fun setOthersSortOrder(order: Int) {
        dbHandler.setPreferredSortOthers(order.toString())
    }

    override suspend fun getUserCredentials(): UserCredentials? = dbHandler.credentials

    override suspend fun doCredentialsExist(): Boolean = dbHandler.credentials != null

    override suspend fun doPreferencesExist(): Boolean = dbHandler.preferences != null

    override suspend fun shouldClearSyncRecords(): Boolean = dbHandler.shouldClearCamsyncRecords()

    override suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo? =
        dbHandler.findNonContactByHandle(userHandle.toString())

    override suspend fun setNonContactEmail(userHandle: Long, email: String) {
        dbHandler.setNonContactEmail(email, userHandle.toString())
    }

    override suspend fun getContactByEmail(email: String?) =
        dbHandler.findContactByEmail(email)

    override suspend fun setUserHasLoggedIn() {
        dbHandler.setFirstTime(false)
    }

    override suspend fun getStorageAskAlways(): Boolean =
        dbHandler.preferences?.storageAskAlways?.toBoolean() ?: true

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) {
        dbHandler.setStorageAskAlways(isStorageAskAlways)
    }

    override suspend fun getStorageDownloadLocation(): String? =
        dbHandler.preferences?.storageDownloadLocation

    override suspend fun setStorageDownloadLocation(storageDownloadLocation: String) {
        dbHandler.setStorageDownloadLocation(storageDownloadLocation)
    }

    override fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean) {
        dbHandler.isPasscodeLockEnabled = isPasscodeLockEnabled
    }

    override suspend fun setPasscodeLockCode(passcodeLockCode: String) {
        dbHandler.passcodeLockCode = passcodeLockCode
    }

    override suspend fun setShowCopyright(showCopyrights: Boolean) {
        dbHandler.setShowCopyright(showCopyrights)
    }

    override suspend fun getAttributes(): MegaAttributes? =
        dbHandler.attributes

    override suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean) {
        dbHandler.saveShouldClearCamsyncRecords(clearCamSyncRecords)
    }

    override suspend fun getChatFilesFolderHandle() = dbHandler.myChatFilesFolderHandle

    override suspend fun saveMyFirstName(firstName: String) =
        dbHandler.saveMyFirstName(firstName)

    override suspend fun saveMyLastName(lastName: String) =
        dbHandler.saveMyLastName(lastName)

    override suspend fun setLastPublicHandle(handle: Long) = dbHandler.setLastPublicHandle(handle)

    override suspend fun setLastPublicHandleTimeStamp() = dbHandler.setLastPublicHandleTimeStamp()

    override suspend fun setLastPublicHandleType(type: Int) {
        dbHandler.lastPublicHandleType = type
    }

    override suspend fun getChatSettings(): ChatSettings? = dbHandler.chatSettings

    override suspend fun setChatSettings(chatSettings: ChatSettings) {
        dbHandler.chatSettings = chatSettings
    }

    override suspend fun saveCredentials(userCredentials: UserCredentials) =
        dbHandler.saveCredentials(userCredentials)

    override suspend fun clearCredentials() = dbHandler.clearCredentials()

    override suspend fun clearPreferences() = dbHandler.clearPreferences()

    override suspend fun setFirstTime(firstTime: Boolean) = dbHandler.setFirstTime(firstTime)

    override suspend fun getFirstTime(): Boolean? =
        dbHandler.preferences?.firstTime?.toBooleanStrictOrNull()

    override suspend fun clearContacts() = dbHandler.clearContacts()

    override suspend fun clearNonContacts() = dbHandler.clearNonContacts()

    override suspend fun clearChatItems() = dbHandler.clearChatItems()

    override suspend fun clearAttributes() = dbHandler.clearAttributes()

    override suspend fun clearChatSettings() = dbHandler.clearChatSettings()

    override suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<Offline> = dbHandler.getOfflineInformationList(path, searchQuery)

    override suspend fun findPendingMessagesNotSent(chatId: Long) =
        dbHandler.findPendingMessagesNotSent(chatId)

    override suspend fun updatePendingMessage(
        idMessage: Long,
        transferTag: Int,
        nodeHandle: String?,
        state: Int,
    ) = dbHandler.updatePendingMessage(idMessage, transferTag, nodeHandle, state)

    override suspend fun setTransferQueueStatus(isPause: Boolean) {
        dbHandler.transferQueueStatus = isPause
    }

    override suspend fun getTransferQueueStatus() = dbHandler.transferQueueStatus

    override fun removePendingMessageByChatId(chatId: Long) =
        dbHandler.removePendingMessageByChatId(chatId)
}
