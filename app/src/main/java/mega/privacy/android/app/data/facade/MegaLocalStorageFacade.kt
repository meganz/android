package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import javax.inject.Inject

/**
 * Mega preferences facade
 *
 * Implements [MegaLocalStorageGateway] and provides a facade over [DatabaseHandler]
 *
 * @property dbHandler
 */
class MegaLocalStorageFacade @Inject constructor(
    val dbHandler: DatabaseHandler
) : MegaLocalStorageGateway {

    override suspend fun getCamSyncHandle(): Long? =
        dbHandler.preferences?.camSyncHandle?.toLongOrNull()

    override suspend fun getMegaHandleSecondaryFolder(): Long? =
        dbHandler.preferences?.megaHandleSecondaryFolder?.toLongOrNull()

    override suspend fun getCloudSortOrder(): Int =
        dbHandler.preferences?.preferredSortCloud?.toInt() ?: ORDER_DEFAULT_ASC
}