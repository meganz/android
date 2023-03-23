package mega.privacy.android.data.facade

import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequestListenerInterface
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

    override var accountAuth: String?
        get() = megaApiFolder.accountAuth
        set(value) {
            megaApiFolder.accountAuth = value
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

    override suspend fun getChildren(parent: MegaNode, order: Int): List<MegaNode> =
        megaApiFolder.getChildren(parent, order)

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

    override suspend fun getChildrenByNode(parentNode: MegaNode, order: Int?): List<MegaNode> =
        if (order == null)
            megaApiFolder.getChildren(parentNode)
        else
            megaApiFolder.getChildren(parentNode, order)

    override suspend fun getParentNode(node: MegaNode): MegaNode? =
        megaApiFolder.getParentNode(node)
}