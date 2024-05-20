package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaSearchPage
import javax.inject.Inject

/**
 * Mega api folder facade
 *
 * Implements [MegaApiFolderGateway] and provides a facade over [MegaApiAndroid]
 *
 * @property megaApiFolder
 */
internal class MegaApiFolderFacade @Inject constructor(
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
) : MegaApiFolderGateway {

    override suspend fun setAccountAuth(authentication: String?) {
        megaApiFolder.accountAuth = authentication
    }

    override suspend fun authorizeNode(handle: Long): MegaNode? =
        megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle))

    override suspend fun authorizeNode(node: MegaNode): MegaNode? =
        megaApiFolder.authorizeNode(node)

    override suspend fun getMegaNodeByHandle(nodeHandle: Long): MegaNode? =
        megaApiFolder.getNodeByHandle(nodeHandle)

    override suspend fun httpServerGetLocalLink(node: MegaNode): String? =
        megaApiFolder.httpServerGetLocalLink(node)

    override suspend fun httpServerIsRunning(): Int = megaApiFolder.httpServerIsRunning()

    override suspend fun httpServerStart(): Boolean = megaApiFolder.httpServerStart()

    override suspend fun httpServerSetMaxBufferSize(bufferSize: Int) =
        megaApiFolder.httpServerSetMaxBufferSize(bufferSize)

    override suspend fun httpServerStop() = megaApiFolder.httpServerStop()

    override suspend fun getRootNode(): MegaNode? = megaApiFolder.rootNode

    override fun getThumbnail(
        node: MegaNode,
        thumbnailFilePath: String,
        listener: MegaRequestListenerInterface?,
    ) = megaApiFolder.getThumbnail(node, thumbnailFilePath, listener)

    override fun fetchNodes(listener: MegaRequestListenerInterface) =
        megaApiFolder.fetchNodes(listener)

    override fun loginToFolder(folderLink: String, listener: MegaRequestListenerInterface) =
        megaApiFolder.loginToFolder(folderLink, listener)

    override fun removeRequestListener(listener: MegaRequestListenerInterface) =
        megaApiFolder.removeRequestListener(listener)

    override suspend fun getNumChildFolders(node: MegaNode): Int =
        megaApiFolder.getNumChildFolders(node)

    override suspend fun getNumChildFiles(node: MegaNode): Int =
        megaApiFolder.getNumChildFiles(node)

    override suspend fun getParentNode(node: MegaNode): MegaNode? =
        megaApiFolder.getParentNode(node)

    override fun getPublicLinkInformation(
        megaFolderLink: String,
        listener: MegaRequestListenerInterface,
    ) = megaApiFolder.getPublicLinkInformation(megaFolderLink, listener)

    override suspend fun setPublicKeyPinning(enable: Boolean) =
        megaApiFolder.setPublicKeyPinning(enable)

    override suspend fun changeApiUrl(apiURL: String, disablePkp: Boolean) =
        megaApiFolder.changeApiUrl(apiURL, disablePkp)

    override fun getFolderInfo(node: MegaNode?, listener: MegaRequestListenerInterface) =
        megaApiFolder.getFolderInfo(node, listener)

    override suspend fun getChildren(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?,
    ): List<MegaNode> = megaApiFolder.getChildren(filter, order, megaCancelToken, megaSearchPage)

    override suspend fun search(
        filter: MegaSearchFilter,
        order: Int,
        megaCancelToken: MegaCancelToken,
        megaSearchPage: MegaSearchPage?,
    ): List<MegaNode> = megaApiFolder.search(filter, order, megaCancelToken, megaSearchPage)
}