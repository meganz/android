package mega.privacy.android.data.facade

import dagger.Lazy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.qualifier.DatabaseDispatcher
import nz.mega.sdk.MegaApiJava.ORDER_CREATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_CREATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_LINK_CREATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC
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
    @DatabaseDispatcher private val databaseDispatcher: CoroutineDispatcher,
) : MegaLocalStorageGateway {

    override suspend fun getCloudSortOrder(): Int = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC
    }

    override suspend fun getCameraSortOrder(): Int = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.preferredSortCameraUpload?.toInt() ?: ORDER_MODIFICATION_DESC
    }

    override suspend fun getOthersSortOrder(): Int = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.preferredSortOthers?.toInt() ?: ORDER_DEFAULT_ASC
    }

    override suspend fun getLinksSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_CREATION_ASC -> ORDER_LINK_CREATION_ASC
            ORDER_CREATION_DESC -> ORDER_LINK_CREATION_DESC
            else -> order
        }

    /**
     * Since offline nodes cannot be ordered by labels and favorites, the offline order will be same as
     * cloud order except when cloud order is ORDER_LABEL_ASC or ORDER_FAV_ASC where it defaults to
     * ORDER_DEFAULT_ASC.
     */
    override suspend fun getOfflineSortOrder(): Int =
        when (val order = getCloudSortOrder()) {
            ORDER_DEFAULT_ASC,
            ORDER_DEFAULT_DESC,
            ORDER_SIZE_ASC,
            ORDER_SIZE_DESC,
            ORDER_MODIFICATION_ASC,
            ORDER_MODIFICATION_DESC,
                -> order

            else -> ORDER_DEFAULT_ASC
        }

    override suspend fun setOfflineSortOrder(order: Int) = withContext(databaseDispatcher) {
        dbHandler.get().setPreferredSortCloud(order.toString())
    }

    override suspend fun setCloudSortOrder(order: Int) = withContext(databaseDispatcher) {
        dbHandler.get().setPreferredSortCloud(order.toString())
    }

    override suspend fun setCameraSortOrder(order: Int) = withContext(databaseDispatcher) {
        dbHandler.get().setPreferredSortCameraUpload(order.toString())
    }

    override suspend fun setOthersSortOrder(order: Int) = withContext(databaseDispatcher) {
        dbHandler.get().setPreferredSortOthers(order.toString())
    }

    override suspend fun doPreferencesExist(): Boolean = withContext(databaseDispatcher) {
        dbHandler.get().preferences != null
    }

    override suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo? =
        withContext(databaseDispatcher) {
            dbHandler.get().findNonContactByHandle(userHandle.toString())
        }

    override suspend fun setNonContactEmail(userHandle: Long, email: String) {
        withContext(databaseDispatcher) {
            dbHandler.get().setNonContactEmail(email, userHandle.toString())
        }
    }

    override suspend fun setNonContactFirstName(userHandle: Long, firstName: String?) {
        withContext(databaseDispatcher) {
            dbHandler.get().setNonContactFirstName(firstName, userHandle.toString())
        }
    }

    override suspend fun setNonContactLastName(userHandle: Long, lastName: String?) {
        withContext(databaseDispatcher) {
            dbHandler.get().setNonContactLastName(lastName, userHandle.toString())
        }
    }

    override suspend fun getContactByEmail(email: String?) = withContext(databaseDispatcher) {
        dbHandler.get().findContactByEmail(email)
    }

    override suspend fun setUserHasLoggedIn() = withContext(databaseDispatcher) {
        dbHandler.get().setFirstTime(false)
    }

    override suspend fun getDownloadLocation(): String? = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.storageDownloadLocation
    }

    override suspend fun isAskForDownloadLocation(): Boolean = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.storageAskAlways?.toBoolean() ?: true
    }

    override suspend fun setAskForDownloadLocation(isStorageAskAlways: Boolean) =
        withContext(databaseDispatcher) {
            dbHandler.get().setStorageAskAlways(isStorageAskAlways)
        }

    override suspend fun isShouldPromptToSaveDestination(): Boolean =
        withContext(databaseDispatcher) {
            dbHandler.get().askSetDownloadLocation
        }

    override suspend fun setShouldPromptToSaveDestination(value: Boolean) =
        withContext(databaseDispatcher) {
            dbHandler.get().askSetDownloadLocation = value
        }

    override suspend fun setDownloadLocation(downloadLocation: String?) =
        withContext(databaseDispatcher) {
            dbHandler.get().setStorageDownloadLocation(downloadLocation)
        }

    override suspend fun isAskBeforeLargeDownloads() = withContext(databaseDispatcher) {
        dbHandler.get().attributes?.askSizeDownload?.equals(true.toString()) ?: true
    }

    override suspend fun setAskBeforeLargeDownloads(askForConfirmation: Boolean) =
        withContext(databaseDispatcher) {
            dbHandler.get().setAttrAskSizeDownload(askForConfirmation.toString())
        }

    override suspend fun setShowCopyright(showCopyrights: Boolean) =
        withContext(databaseDispatcher) {
            dbHandler.get().setShowCopyright(showCopyrights)
        }

    override suspend fun getAttributes(): MegaAttributes? = withContext(databaseDispatcher) {
        dbHandler.get().attributes
    }

    override suspend fun resetAccountDetailsTimeStamp() = withContext(databaseDispatcher) {
        dbHandler.get().resetAccountDetailsTimeStamp()
    }

    override suspend fun resetExtendedAccountDetailsTimestamp() = withContext(databaseDispatcher) {
        dbHandler.get().resetExtendedAccountDetailsTimestamp()
    }

    override suspend fun getChatFilesFolderHandle() = withContext(databaseDispatcher) {
        dbHandler.get().myChatFilesFolderHandle
    }

    override suspend fun setLastPublicHandle(handle: Long) = withContext(databaseDispatcher) {
        dbHandler.get().setLastPublicHandle(handle)
    }

    override suspend fun setLastPublicHandleTimeStamp() = withContext(databaseDispatcher) {
        dbHandler.get().setLastPublicHandleTimeStamp()
    }

    override suspend fun setLastPublicHandleType(type: Int) = withContext(databaseDispatcher) {
        dbHandler.get().lastPublicHandleType = type
    }

    override suspend fun clearPreferences() = withContext(databaseDispatcher) {
        dbHandler.get().clearPreferences()
    }

    override suspend fun setFirstTime(firstTime: Boolean) = withContext(databaseDispatcher) {
        dbHandler.get().setFirstTime(firstTime)
    }

    override suspend fun getFirstTime(): Boolean? = withContext(databaseDispatcher) {
        dbHandler.get().preferences?.firstTime?.toBooleanStrictOrNull()
    }

    override suspend fun clearContacts() = withContext(databaseDispatcher) {
        dbHandler.get().clearContacts()
    }

    override suspend fun clearNonContacts() = withContext(databaseDispatcher) {
        dbHandler.get().clearNonContacts()
    }

    override suspend fun clearChatItems() = withContext(databaseDispatcher) {
        dbHandler.get().clearChatItems()
    }

    override suspend fun clearAttributes() = withContext(databaseDispatcher) {
        dbHandler.get().clearAttributes()
    }

    override suspend fun setTransferQueueStatus(isPause: Boolean) =
        withContext(databaseDispatcher) {
            dbHandler.get().transferQueueStatus = isPause
        }

    override suspend fun getTransferQueueStatus() = withContext(databaseDispatcher) {
        dbHandler.get().transferQueueStatus
    }

    override suspend fun shouldShowCopyright(): Boolean = withContext(databaseDispatcher) {
        dbHandler.get().shouldShowCopyright
    }
}
