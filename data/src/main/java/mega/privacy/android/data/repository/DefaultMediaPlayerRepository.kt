package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.getThumbnailFileName
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.FileTypeInfoMapper
import mega.privacy.android.data.mapper.NodeMapper
import mega.privacy.android.data.model.MimeTypeList
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.AddNodeType
import nz.mega.sdk.MegaApiJava.FILE_TYPE_AUDIO
import nz.mega.sdk.MegaApiJava.FILE_TYPE_VIDEO
import nz.mega.sdk.MegaApiJava.SEARCH_TARGET_ROOTNODE
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import java.io.File
import javax.inject.Inject

/**
 * Implementation of MediaPlayerRepository
 */
internal class DefaultMediaPlayerRepository @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    private val dbHandler: DatabaseHandler,
    private val nodeMapper: NodeMapper,
    private val cacheFolder: CacheFolderGateway,
    private val fileTypeInfoMapper: FileTypeInfoMapper,
    private val addNodeType: AddNodeType,
    private val fileGateway: FileGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MediaPlayerRepository {

    override suspend fun getTypedNodeByHandle(handle: Long): TypedNode? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(handle)?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getLocalLinkFromMegaApiFolder(nodeHandle: Long): String? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                megaApiFolder.httpServerGetLocalLink(megaNode)
            }
        }

    override suspend fun getLocalLinkFromMegaApi(nodeHandle: Long): String? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(nodeHandle)?.let { megaNode ->
                megaApi.httpServerGetLocalLink(megaNode)
            }
        }

    override suspend fun getAudioNodes(order: Int): List<TypedNode> =
        withContext(ioDispatcher) {
            megaApi.searchByType(MegaCancelToken.createInstance(),
                order,
                FILE_TYPE_AUDIO,
                SEARCH_TARGET_ROOTNODE).filter {
                it.isFile && filterByNodeName(true, it.name)
            }.map { megaNode ->
                convertToTypedNode(megaNode)
            }
        }


    override suspend fun getVideoNodes(order: Int): List<TypedNode> =
        withContext(ioDispatcher) {
            megaApi.searchByType(MegaCancelToken.createInstance(),
                order,
                FILE_TYPE_VIDEO,
                SEARCH_TARGET_ROOTNODE).filter {
                it.isFile && filterByNodeName(false, it.name)
            }.map { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getThumbnail(
        isMegaApiFolder: Boolean,
        nodeHandle: Long,
        path: String,
        finishedCallback: (nodeHandle: Long) -> Unit,
    ) {
        withContext(ioDispatcher) {
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { megaRequest, _ ->
                    finishedCallback(megaRequest.nodeHandle)
                })
            if (isMegaApiFolder) {
                megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                    megaApiFolder.getThumbnail(
                        node = node,
                        thumbnailFilePath = path,
                        listener = listener
                    )
                }
            } else {
                megaApi.getMegaNodeByHandle(nodeHandle)?.let { node ->
                    megaApi.getThumbnail(
                        node = node,
                        thumbnailFilePath = path,
                        listener = listener
                    )
                }
            }
        }
    }

    override suspend fun credentialsIsNull(): Boolean = dbHandler.credentials == null

    override suspend fun getRubbishNode(): TypedNode? =
        withContext(ioDispatcher) {
            megaApi.getRubbishBinNode()?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getInboxNode(): TypedNode? =
        withContext(ioDispatcher) {
            megaApi.getInboxNode()?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getRootNode(): TypedNode? =
        withContext(ioDispatcher) {
            megaApi.getRootNode()?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun megaApiFolderGetRootNode(): TypedNode? =
        withContext(ioDispatcher) {
            megaApiFolder.getRootNode()?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getParentNodeByHandle(parentHandle: Long): TypedNode? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun megaApiFolderGetParentNode(parentHandle: Long): TypedNode? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { megaNode ->
                convertToTypedNode(megaNode)
            }
        }

    override suspend fun getChildrenByParentHandle(
        isAudio: Boolean,
        parentHandle: Long,
        order: Int,
    ): List<TypedNode>? =
        withContext(ioDispatcher) {
            megaApi.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApi.getChildren(parent, order).filter {
                    it.isFile && filterByNodeName(isAudio, it.name)
                }.map { node ->
                    convertToTypedNode(node)
                }
            }
        }

    override suspend fun megaApiFolderGetChildrenByParentHandle(
        isAudio: Boolean,
        parentHandle: Long,
        order: Int,
    ): List<TypedNode>? =
        withContext(ioDispatcher) {
            megaApiFolder.getMegaNodeByHandle(parentHandle)?.let { parent ->
                megaApiFolder.getChildren(parent, order).filter {
                    it.isFile && filterByNodeName(isAudio, it.name)
                }.map { node ->
                    convertToTypedNode(node)
                }
            }
        }

    override suspend fun getNodesFromPublicLinks(isAudio: Boolean, order: Int): List<TypedNode> =
        withContext(ioDispatcher) {
            megaApi.getPublicLinks(order).filter {
                it.isFile && filterByNodeName(isAudio, it.name)
            }.map { node ->
                convertToTypedNode(node)
            }
        }

    override suspend fun getNodesFromInShares(isAudio: Boolean, order: Int): List<TypedNode> =
        withContext(ioDispatcher) {
            megaApi.getInShares(order).filter {
                it.isFile && filterByNodeName(isAudio, it.name)
            }.map { node ->
                convertToTypedNode(node)
            }
        }

    override suspend fun getNodesFromOutShares(
        isAudio: Boolean,
        lastHandle: Long,
        order: Int,
    ): List<TypedNode> =
        withContext(ioDispatcher) {
            var handle: Long? = lastHandle
            val result = mutableListOf<MegaNode>()
            megaApi.getOutShares(order).map { megaShare ->
                megaApi.getMegaNodeByHandle(megaShare.nodeHandle)
            }.let { nodes ->
                for (node in nodes) {
                    if (node != null && node.handle != handle) {
                        handle = node.handle
                        result.add(node)
                    }
                }
            }
            result.filter { megaNode ->
                megaNode.isFile && filterByNodeName(isAudio, megaNode.name)
            }.map { node ->
                convertToTypedNode(node)
            }
        }

    override suspend fun getNodesByEmail(isAudio: Boolean, email: String): List<TypedNode>? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                megaApi.getInShares(megaUser).filter { megaNode ->
                    megaNode.isFile && filterByNodeName(isAudio, megaNode.name)
                }.map { node ->
                    convertToTypedNode(node)
                }
            }
        }

    override suspend fun getUserNameByEmail(email: String): String? =
        withContext(ioDispatcher) {
            megaApi.getContact(email)?.let { megaUser ->
                getMegaUserNameDB(megaUser)
            }
        }

    override suspend fun getNodesByHandles(
        isAudio: Boolean,
        handles: List<Long>,
    ): List<TypedNode> =
        handles.mapNotNull { handle ->
            megaApi.getMegaNodeByHandle(handle)
        }.map { node ->
            convertToTypedNode(node)
        }

    override suspend fun megaApiHttpServerStop() = megaApi.httpServerStop()

    override suspend fun megaApiFolderHttpServerStop() = megaApiFolder.httpServerStop()

    override suspend fun megaApiHttpServerIsRunning(): Int = megaApi.httpServerIsRunning()

    override suspend fun megaApiFolderHttpServerIsRunning(): Int =
        megaApiFolder.httpServerIsRunning()

    override suspend fun megaApiHttpServerStart() = megaApi.httpServerStart()

    override suspend fun megaApiFolderHttpServerStart() = megaApiFolder.httpServerStart()

    override suspend fun megaApiHttpServerSetMaxBufferSize(bufferSize: Int) =
        megaApi.httpServerSetMaxBufferSize(bufferSize)

    override suspend fun megaApiFolderHttpServerSetMaxBufferSize(bufferSize: Int) =
        megaApiFolder.httpServerSetMaxBufferSize(bufferSize)

    override suspend fun getFingerprint(filePath: String): String? =
        megaApi.getFingerprint(filePath)

    override suspend fun getLocalFilePath(typedFileNode: TypedFileNode?): String? =
        fileGateway.getLocalFilePath(typedFileNode)

    private fun filterByNodeName(isAudio: Boolean, name: String): Boolean =
        MimeTypeList.typeForName(name).let { (type, extension) ->
            if (isAudio) {
                isAudio(type, extension) && !isAudioNotSupported(extension)
            } else {
                isVideo(type, extension) &&
                        isVideoReproducible(type, extension) &&
                        !isVideoNotSupported(extension)
            }
        }

    /*
     * Check is MimeType of audio type
     */
    private fun isAudio(type: String, extension: String): Boolean =
        type.startsWith("audio/") ||
                extension == "opus" ||
                extension == "weba"


    private fun isAudioNotSupported(extension: String): Boolean =
        extension == "wma" ||
                extension == "aif" ||
                extension == "aiff" ||
                extension == "iff" ||
                extension == "oga" ||
                extension == "3ga"

    /*
     * Check is MimeType of video type
     */
    private fun isVideo(type: String, extension: String): Boolean =
        type.startsWith("video/") || extension == "mkv"

    private fun isVideoReproducible(type: String, extension: String): Boolean =
        type.startsWith("video/") ||
                extension == "mkv" ||
                extension == "flv" ||
                extension == "vob" ||
                extension == "avi" ||
                extension == "wmv" ||
                extension == "mpg" ||
                extension == "mts"

    private fun isVideoNotSupported(extension: String): Boolean =
        extension == "mpg" || extension == "avi" || extension == "wmv"

    /**
     * Get the thumbnail cache file path
     * @param megaNode MegaNode
     * @return thumbnail cache file path
     */
    private fun getThumbnailCacheFilePath(megaNode: MegaNode) =
        cacheFolder.getCacheFolder(CacheFolderConstant.THUMBNAIL_FOLDER)?.let { thumbnail ->
            "$thumbnail${File.separator}${megaNode.getThumbnailFileName()}"
        }

    private fun getMegaUserNameDB(user: MegaUser): String? =
        dbHandler.findContactByHandle(user.handle.toString())?.let { megaContactDB ->
            when {
                megaContactDB.nickname.isNullOrEmpty().not() -> {
                    megaContactDB.nickname
                }
                megaContactDB.name.isNullOrEmpty().not() -> {
                    if (megaContactDB.lastName.isNullOrEmpty().not()) {
                        "$megaContactDB.name $megaContactDB.lastName"
                    } else {
                        megaContactDB.name
                    }
                }
                megaContactDB.lastName.isNullOrEmpty().not() -> {
                    megaContactDB.lastName
                }
                else -> {
                    megaContactDB.mail
                }
            } ?: user.email
        }

    private suspend fun convertToTypedNode(node: MegaNode): TypedNode =
        addNodeType(
            nodeMapper(
                node,
                ::getThumbnailCacheFilePath,
                megaApi::hasVersion,
                megaApi::getNumChildFolders,
                megaApi::getNumChildFiles,
                fileTypeInfoMapper,
                megaApi::isPendingShare,
                megaApi::isInRubbish,
            )
        )

}