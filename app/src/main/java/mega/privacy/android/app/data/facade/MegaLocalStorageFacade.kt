package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
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

    override val camSyncHandle: Long? = dbHandler.preferences?.camSyncHandle?.toLongOrNull()

    override val megaHandleSecondaryFolder: Long? =
        dbHandler.preferences?.megaHandleSecondaryFolder?.toLongOrNull()

}