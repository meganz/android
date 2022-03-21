package mega.privacy.android.app.data.facade

import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

/**
 * Mega api facade
 *
 * Implements [MegaApiGateway] and provides a facade over [MegaApiAndroid]
 *
 * @property megaApi
 */
class MegaApiFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : MegaApiGateway {
    override fun multiFactorAuthAvailable(): Boolean {
        return megaApi.multiFactorAuthAvailable()
    }

    override fun multiFactorAuthEnabled(email: String?, listener: MegaRequestListenerInterface?) {
        megaApi.multiFactorAuthCheck(email, listener)
    }

    override fun cancelAccount(listener: MegaRequestListenerInterface?) {
        megaApi.cancelAccount(listener)
    }

    override val accountEmail: String?
        get() = megaApi.myEmail
    override val isBusinessAccount: Boolean
        get() = megaApi.isBusinessAccount
    override val isMasterBusinessAccount: Boolean
        get() = megaApi.isMasterBusinessAccount
    override val rootNode: MegaNode?
        get() = megaApi.rootNode
}