package mega.privacy.android.app.data.facade

import mega.privacy.android.app.di.MegaApiFolder
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Mega api folder facade
 *
 * Implements [MegaApiFolderGateway] and provides a facade over [MegaApiAndroid]
 *
 * @property megaApiFolder
 */
class MegaApiFolderFacade @Inject constructor(
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
) : MegaApiFolderGateway {

    override var accountAuth: String
        get() = megaApiFolder.accountAuth
        set(value) {
            megaApiFolder.accountAuth = value
        }

    override suspend fun authorizeNode(handle: Long): MegaNode? =
        megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle))
}