package mega.privacy.android.data.facade

import dagger.Lazy
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.settings.ChatSettings
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
    private val dbHandler: Lazy<DatabaseHandler>,
) : MegaLocalStorageGateway {

    override suspend fun getCloudSortOrder(): Int =
        dbHandler.get().preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getCameraSortOrder(): Int =
        dbHandler.get().preferences?.preferredSortCameraUpload?.toInt() ?: ORDER_MODIFICATION_DESC

    override suspend fun getOthersSortOrder(): Int =
        dbHandler.get().preferences?.preferredSortOthers?.toInt() ?: ORDER_DEFAULT_ASC

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
        dbHandler.get().setPreferredSortCloud(order.toString())
    }

    override suspend fun setCloudSortOrder(order: Int) {
        dbHandler.get().setPreferredSortCloud(order.toString())
    }

    override suspend fun setCameraSortOrder(order: Int) {
        dbHandler.get().setPreferredSortCameraUpload(order.toString())
    }

    override suspend fun setOthersSortOrder(order: Int) {
        dbHandler.get().setPreferredSortOthers(order.toString())
    }

    override suspend fun doPreferencesExist(): Boolean = dbHandler.get().preferences != null

    override suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo? =
        dbHandler.get().findNonContactByHandle(userHandle.toString())

    override suspend fun setNonContactEmail(userHandle: Long, email: String) {
        dbHandler.get().setNonContactEmail(email, userHandle.toString())
    }

    override suspend fun getContactByEmail(email: String?) =
        dbHandler.get().findContactByEmail(email)

    override suspend fun setUserHasLoggedIn() {
        dbHandler.get().setFirstTime(false)
    }

    override suspend fun getStorageDownloadLocation(): String? =
        dbHandler.get().preferences?.storageDownloadLocation

    override suspend fun isStorageAskAlways(): Boolean =
        dbHandler.get().preferences?.storageAskAlways?.toBoolean() ?: true

    override suspend fun setStorageAskAlways(isStorageAskAlways: Boolean) {
        dbHandler.get().setStorageAskAlways(isStorageAskAlways)
    }

    override suspend fun isAskSetDownloadLocation(): Boolean =
        dbHandler.get().askSetDownloadLocation

    override suspend fun setAskSetDownloadLocation(value: Boolean) {
        dbHandler.get().askSetDownloadLocation = value
    }

    override suspend fun setStorageDownloadLocation(storageDownloadLocation: String) {
        dbHandler.get().setStorageDownloadLocation(storageDownloadLocation)
    }

    override suspend fun isAskBeforeLargeDownloads() =
        dbHandler.get().attributes?.askSizeDownload?.equals(true.toString()) ?: true

    override suspend fun setAskBeforeLargeDownloads(askForConfirmation: Boolean) {
        dbHandler.get().setAttrAskSizeDownload(askForConfirmation.toString())
    }

    override fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean) {
        dbHandler.get().isPasscodeLockEnabled = isPasscodeLockEnabled
    }

    override suspend fun setPasscodeLockCode(passcodeLockCode: String) {
        dbHandler.get().passcodeLockCode = passcodeLockCode
    }

    override suspend fun setShowCopyright(showCopyrights: Boolean) {
        dbHandler.get().setShowCopyright(showCopyrights)
    }

    override suspend fun getAttributes(): MegaAttributes? =
        dbHandler.get().attributes

    override suspend fun getChatFilesFolderHandle() = dbHandler.get().myChatFilesFolderHandle

    override suspend fun setLastPublicHandle(handle: Long) = dbHandler.get().setLastPublicHandle(handle)

    override suspend fun setLastPublicHandleTimeStamp() = dbHandler.get().setLastPublicHandleTimeStamp()

    override suspend fun setLastPublicHandleType(type: Int) {
        dbHandler.get().lastPublicHandleType = type
    }

    override suspend fun getChatSettings(): ChatSettings? = dbHandler.get().chatSettings

    override suspend fun setChatSettings(chatSettings: ChatSettings) {
        dbHandler.get().chatSettings = chatSettings
    }

    override suspend fun clearPreferences() = dbHandler.get().clearPreferences()

    override suspend fun setFirstTime(firstTime: Boolean) = dbHandler.get().setFirstTime(firstTime)

    override suspend fun getFirstTime(): Boolean? =
        dbHandler.get().preferences?.firstTime?.toBooleanStrictOrNull()

    override suspend fun clearContacts() = dbHandler.get().clearContacts()

    override suspend fun clearNonContacts() = dbHandler.get().clearNonContacts()

    override suspend fun clearChatItems() = dbHandler.get().clearChatItems()

    override suspend fun clearAttributes() = dbHandler.get().clearAttributes()

    override suspend fun clearChatSettings() = dbHandler.get().clearChatSettings()

    override suspend fun setTransferQueueStatus(isPause: Boolean) {
        dbHandler.get().transferQueueStatus = isPause
    }

    override suspend fun getTransferQueueStatus() = dbHandler.get().transferQueueStatus

    override fun removePendingMessageByChatId(chatId: Long) =
        dbHandler.get().removePendingMessageByChatId(chatId)

    override fun shouldShowCopyright(): Boolean = dbHandler.get().shouldShowCopyright
}
