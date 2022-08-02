package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.main.megachat.NonContactInfo
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import javax.inject.Inject

/**
 * Mega preferences facade
 *
 * Implements [MegaLocalStorageGateway] and provides a facade over [DatabaseHandler]
 *
 * @property dbHandler
 */
class MegaLocalStorageFacade @Inject constructor(
    val dbHandler: DatabaseHandler,
) : MegaLocalStorageGateway {

    override suspend fun getCamSyncHandle(): Long? =
        dbHandler.preferences?.camSyncHandle?.toLongOrNull()

    override suspend fun getMegaHandleSecondaryFolder(): Long? =
        dbHandler.preferences?.megaHandleSecondaryFolder?.toLongOrNull()

    override suspend fun getCloudSortOrder(): Int =
        dbHandler.preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getCameraSortOrder(): Int =
        dbHandler.preferences?.preferredSortCameraUpload?.toInt() ?: ORDER_MODIFICATION_DESC

    override suspend fun getOthersSortOrder(): Int =
        dbHandler.preferences?.preferredSortOthers?.toInt() ?: ORDER_DEFAULT_ASC

    override suspend fun getUserCredentials(): UserCredentials? = dbHandler.credentials

    override suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo? =
        dbHandler.findNonContactByHandle(userHandle.toString())

    override suspend fun setNonContactEmail(userHandle: Long, email: String) {
        dbHandler.setNonContactEmail(email, userHandle.toString())
    }
}