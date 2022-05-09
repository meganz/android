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

    override val camSyncHandle: String? = dbHandler.preferences?.camSyncHandle

    override val megaHandleSecondaryFolder: String? =
        dbHandler.preferences?.megaHandleSecondaryFolder

}