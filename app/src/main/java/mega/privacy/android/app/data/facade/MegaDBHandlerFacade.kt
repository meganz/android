package mega.privacy.android.app.data.facade

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.data.gateway.api.MegaDBHandlerGateway
import javax.inject.Inject

/**
 * Mega preferences facade
 *
 * Implements [MegaDBHandlerGateway] and provides a facade over [DatabaseHandler]
 *
 * @property dbHandler
 */
class MegaDBHandlerFacade @Inject constructor(
    val dbHandler: DatabaseHandler
) : MegaDBHandlerGateway {

    override val camSyncHandle: String? = dbHandler.preferences?.camSyncHandle

    override val megaHandleSecondaryFolder: String? =
        dbHandler.preferences?.megaHandleSecondaryFolder

}