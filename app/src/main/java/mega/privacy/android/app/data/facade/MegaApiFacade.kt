package mega.privacy.android.app.data.facade

import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.di.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaLoggerInterface
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaGlobalListenerInterface
import nz.mega.sdk.MegaUser
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

    override fun getFavourites(
        node: MegaNode?,
        count: Int,
        listener: MegaRequestListenerInterface?
    ) {
        megaApi.getFavourites(node, count, listener)
    }

    override fun getMegaNodeByHandle(nodeHandle: Long): MegaNode =
        megaApi.getNodeByHandle(nodeHandle)

    override fun hasVersion(node: MegaNode): Boolean = megaApi.hasVersions(node)

    override fun getChildrenByNode(parentNode: MegaNode): ArrayList<MegaNode> =
        megaApi.getChildren(parentNode)

    override fun getNumChildFolders(node: MegaNode): Int = megaApi.getNumChildFolders(node)

    override fun getNumChildFiles(node: MegaNode): Int = megaApi.getNumChildFiles(node)

    override fun setAutoAcceptContactsFromLink(
        disableAutoAccept: Boolean,
        listener: MegaRequestListenerInterface
    ) = megaApi.setContactLinksOption(disableAutoAccept, listener)

    override fun isAutoAcceptContactsFromLinkEnabled(listener: MegaRequestListenerInterface) =
        megaApi.getContactLinksOption(listener)

    override fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface) =
        megaApi.getFolderInfo(node, listener)

    override fun addLogger(logger: MegaLoggerInterface) = MegaApiAndroid.addLoggerObject(logger)

    override fun removeLogger(logger: MegaLoggerInterface) =
        MegaApiAndroid.removeLoggerObject(logger)

    override fun setLogLevel(logLevel: Int) = MegaApiAndroid.setLogLevel(logLevel)

    override fun setUseHttpsOnly(enabled: Boolean) = megaApi.useHttpsOnly(enabled)

    override fun addGlobalListener(listener: MegaGlobalListenerInterface) =
        megaApi.addGlobalListener(listener)

    override fun removeGlobalListener(listener: MegaGlobalListenerInterface) =
        megaApi.removeGlobalListener(listener)

    override suspend fun getLoggedInUser(): MegaUser? = megaApi.myUser
}