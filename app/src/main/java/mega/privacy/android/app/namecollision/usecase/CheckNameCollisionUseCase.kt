package mega.privacy.android.app.namecollision.usecase

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.coroutines.withContext
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.exception.NoPendingCollisionsException
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.MessageDoesNotExistException
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.exception.EmptyFolderException
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Use case for checking name collisions before uploading, copying or moving.
 *
 * @property megaApiGateway                [MegaApiAndroid] instance to check collisions.
 * @property getNodeUseCase         Required for getting nodes.
 * @property getChatMessageUseCase  Required for getting chat [MegaNode]s.
 */
@OptIn(ExperimentalContracts::class)
class CheckNameCollisionUseCase @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val getNodeUseCase: GetNodeUseCase,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == INVALID_HANDLE) megaApiGateway.getRootNode() else megaApiGateway.getMegaNodeByHandle(
            parentHandle
        )


    /**
     * Checks if a node with the same name exists on the provided parent node.
     *
     * @param handle        Handle of the node to check its name.
     * @param parentHandle  Handle of the parent node in which to look.
     * @param type          [NameCollisionType]
     * @return Single Long with the node handle with which there is a name collision.
     */
    suspend fun check(
        handle: Long,
        parentHandle: Long,
        type: NameCollisionType,
    ): NameCollision =
        checkNodeCollisionsWithType(
            node = megaApiGateway.getMegaNodeByHandle(handle),
            parentNode = getParentOrRootNode(parentHandle),
            type = type,
        )

    /**
     * Checks if a node with the same name exists on the provided parent node.
     *
     * @param node          [MegaNode] to check its name.
     * @param parentHandle  Handle of the parent node in which to look.
     * @param type          [NameCollisionType]
     * @return Single Long with the node handle with which there is a name collision.
     */
    suspend fun check(node: MegaNode?, parentHandle: Long, type: NameCollisionType): NameCollision =
        checkNodeCollisionsWithType(
            node = node,
            parentNode = getParentOrRootNode(parentHandle),
            type = type,
        )


    /**
     * Checks if a node with the same name exists on the provided parent node.
     *
     * @param node          [MegaNode] to check its name.
     * @param parentNode    Parent [MegaNode] in which to look.
     * @param type          [NameCollisionType]
     * @return Single Long with the node handle with which there is a name collision.
     */
    fun check(
        node: MegaNode?,
        parentNode: MegaNode?,
        type: NameCollisionType,
    ): Single<NameCollision> =
        rxSingle(ioDispatcher) {
            checkNodeCollisionsWithType(node, parentNode, type)
        }

    private suspend fun checkNodeCollisionsWithType(
        node: MegaNode?,
        parentNode: MegaNode?,
        type: NameCollisionType,
    ): NameCollision {
        if (node == null) throw MegaNodeException.NodeDoesNotExistsException()
        val handle = checkAsync(node.name, parentNode)
        val childCounts = getChildCounts(parentNode)
        return when (type) {
            NameCollisionType.COPY -> NameCollision.Copy.getCopyCollision(
                handle,
                node,
                parentHandle = parentNode.handle,
                childFolderCount = childCounts.first,
                childFileCount = childCounts.second,
            )

            NameCollisionType.MOVE -> NameCollision.Movement.getMovementCollision(
                handle,
                node,
                parentHandle = parentNode.handle,
                childFolderCount = childCounts.first,
                childFileCount = childCounts.second,
            )

            NameCollisionType.UPLOAD -> throw IllegalStateException("UPLOAD collisions are not handled in this method")
        }
    }

    private suspend fun getChildCounts(parentNode: MegaNode) =
        withContext(ioDispatcher) {
            megaApiGateway.getNumChildFolders(parentNode) to megaApiGateway.getNumChildFiles(
                parentNode
            )
        }

    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentHandle  Handle of the parent node in which to look.
     * @return Single Long with the node handle with which there is a name collision.
     */
    fun check(name: String, parentHandle: Long): Single<Long> =
        rxSingle(ioDispatcher) {
            checkAsync(name = name, parent = getParentOrRootNode(parentHandle))
        }

    suspend fun checkNameCollision(name: String, parentHandle: Long): Long =
        withContext(ioDispatcher) {
            checkAsync(
                name = name,
                parent = getParentOrRootNode(parentHandle)
            )
        }


    /**
     * Checks if a node with the given name exists on the provided parent node.
     *
     * @param name          Name of the node.
     * @param parentNode    Parent node in which to look.
     * @return Single Long with the node handle with which there is a name collision.
     */
    private fun check(name: String, parentNode: MegaNode?): Single<Long> =
        rxSingle(ioDispatcher) {
            checkAsync(name, parentNode)
        }

    suspend fun checkAsync(name: String, parent: MegaNode?): Long {
        contract { returns() implies (parent != null) }
        if (parent == null) {
            throw MegaNodeException.ParentDoesNotExistException()
        }

        return withContext(ioDispatcher) { megaApiGateway.getChildNode(parent, name)?.handle }
            ?: throw MegaNodeException.ChildDoesNotExistsException()
    }

    /**
     * Checks a list of handles in order to know which names already exist on the parent nodes.
     *
     * @param nodes         List of node handles to check.
     * @param parentHandle  Parent handle node in which to look.
     * @return Single<Pair<List<NameCollision>, LongArray>> containing:
     *  - First:    List of NameCollision with name collisions.
     *  - Second:   Array of handles without name collision.
     */
    fun checkNodeList(
        nodes: List<MegaNode>,
        parentHandle: Long,
        type: NameCollisionType,
    ): Single<Pair<ArrayList<NameCollision>, List<MegaNode>>> =
        rxSingle(ioDispatcher) {
            checkNodeListAsync(nodes, parentHandle, type)
        }

    /**
     * Check node list async
     *
     * @param nodes
     * @param parentHandle
     * @param type
     * @return
     */
    suspend fun checkNodeListAsync(
        nodes: List<MegaNode>,
        parentHandle: Long,
        type: NameCollisionType,
    ): Pair<ArrayList<NameCollision>, MutableList<MegaNode>> {
        if (nodes.isEmpty()) throw NoPendingCollisionsException()
        return nodes.fold(
            Pair(
                ArrayList(),
                mutableListOf()
            )
        ) { result, node ->
            runCatching {
                checkNodeCollisionsWithType(
                    node = node,
                    parentNode = getParentOrRootNode(parentHandle),
                    type = type,
                )
            }.onFailure {
                Timber.e(it, "No collision.")
                result.second.add(node)
            }.onSuccess {
                result.first.add(it)
            }

            result
        }
    }


    /**
     * Checks a list of handles in order to know which names already exist on the parent nodes.
     *
     * @param handles       List of node handles to check.
     * @param parentHandle  Parent handle node in which to look.
     * @return Single<Pair<List<NameCollision>, LongArray>> containing:
     *  - First:    List of NameCollision with name collisions.
     *  - Second:   Array of handles without name collision.
     */
    fun checkHandleList(
        handles: LongArray,
        parentHandle: Long,
        type: NameCollisionType,
    ): Single<Pair<ArrayList<NameCollision>, LongArray>> =
        rxSingle(ioDispatcher) {
            checkHandleListAsync(handles, parentHandle, type)
        }

    /**
     * Check handle list async
     *
     * @param handles
     * @param parentHandle
     * @param type
     * @return
     */
    suspend fun checkHandleListAsync(
        handles: LongArray,
        parentHandle: Long,
        type: NameCollisionType,
    ): Pair<ArrayList<NameCollision>, LongArray> {
        if (handles.isEmpty()) throw NoPendingCollisionsException()
        val resultPair = handles.fold(
            Pair(
                ArrayList<NameCollision>(),
                mutableListOf<Long>()
            )
        ) { result, handle ->
            runCatching {
                checkNodeCollisionsWithType(
                    node = megaApiGateway.getMegaNodeByHandle(handle),
                    parentNode = getParentOrRootNode(parentHandle),
                    type = type,
                )
            }.onFailure {
                Timber.e(it, "No collision.")
                result.second.add(handle)
            }.onSuccess {
                result.first.add(it)
            }

            result
        }

        return Pair(resultPair.first, resultPair.second.toLongArray())
    }

    /**
     * Checks a list of [MegaNode] in order to know which names already exist on the parent nodes
     * in which them will be restored.
     *
     * @param nodes List of nodes to check.
     * @return Single<Pair<ArrayList<NameCollision>, List<MegaNode>>> containing:
     *  - First:    List of [NameCollision] with name collisions.
     *  - Second:   List of [MegaNode] without name collision.
     */
    fun checkRestorations(
        nodes: List<MegaNode>,
    ): Single<Pair<ArrayList<NameCollision>, List<MegaNode>>> =
        rxSingle(ioDispatcher) {
            checkRestorationsAsync(nodes)
        }

    /**
     * Check restorations async
     *
     * @param nodes
     * @return
     */
    suspend fun checkRestorationsAsync(
        nodes: List<MegaNode>,
    ): Pair<ArrayList<NameCollision>, MutableList<MegaNode>> {
        if (nodes.isEmpty()) throw NoPendingCollisionsException()
        return nodes.fold(
            Pair(
                ArrayList(),
                mutableListOf()
            )
        ) { result, node ->
            val restoreHandle = node.restoreHandle
            val parent = getParentOrRootNode(restoreHandle)
            if (parent == null || megaApiGateway.isInRubbish(parent)) {
                result.second.add(node)
            } else {
                val childCounts = getChildCounts(parent)
                runCatching { checkAsync(node.name, parent) }
                    .onFailure {
                        Timber.e(it, "No collision.")
                        result.second.add(node)
                    }
                    .onSuccess {
                        result.first.add(
                            NameCollision.Movement.getMovementCollision(
                                collisionHandle = it,
                                node = node,
                                parentHandle = restoreHandle,
                                childFileCount = childCounts.second,
                                childFolderCount = childCounts.first,
                            )
                        )
                    }
            }
            result
        }
    }


    /**
     * Checks a list of ShareInfo in order to know which names already exist
     * on the provided parent node.
     *
     * @param shareInfoList    List of ShareInfo to check.
     * @param parentNode    Parent node in which to look.
     * @return Single<Pair<ArrayList<NameCollision>, List<ShareInfo>>> containing:
     *  - First:    List of [NameCollision] with name collisions.
     *  - Second:   List of [ShareInfo] without name collision.
     */
    fun checkShareInfoList(
        shareInfoList: List<ShareInfo>,
        parentNode: MegaNode?,
    ): Single<Pair<ArrayList<NameCollision>, List<ShareInfo>>> =
        rxSingle(ioDispatcher) {
            checkShareInfoAsync(parentNode, shareInfoList)
        }

    /**
     * Check share info async
     *
     * @param parentNode
     * @param shareInfoList
     * @return
     */
    suspend fun checkShareInfoAsync(
        parentNode: MegaNode?,
        shareInfoList: List<ShareInfo>,
    ): Pair<ArrayList<NameCollision>, MutableList<ShareInfo>> {
        if (parentNode == null) {
            throw MegaNodeException.ParentDoesNotExistException()
        }
        return shareInfoList.fold(
            Pair(
                ArrayList(),
                mutableListOf()
            )
        ) { result, shareInfo ->
            runCatching {
                checkAsync(shareInfo.originalFileName, parentNode)
            }.onFailure {
                Timber.e(it, "No collision.")
                result.second.add(shareInfo)
            }.onSuccess {
                result.first.add(
                    NameCollision.Upload.getUploadCollision(
                        it,
                        shareInfo,
                        parentNode.handle
                    )
                )
            }

            result
        }
    }

    /**
     * Checks a list of [FolderContent.Data] in order to know which names already exist
     * on the provided parent node.
     *
     * @param parentHandle  Parent handle of the MegaNode in which the content will be uploaded.
     * @param uploadContent List of [FolderContent.Data] to check.
     * @return Single with the list of collisions if any and the list of [FolderContent.Data]
     * updated with them.
     */
    fun checkFolderUploadList(
        parentHandle: Long,
        uploadContent: MutableList<FolderContent.Data>,
    ): Single<Pair<ArrayList<NameCollision>, MutableList<FolderContent.Data>>> =
        rxSingle(ioDispatcher) {
            return@rxSingle checkFolderUploadListAsync(parentHandle, uploadContent)


        }

    /**
     * Check folder upload list async
     *
     * @param parentHandle
     * @param uploadContent
     * @return
     */
    suspend fun checkFolderUploadListAsync(
        parentHandle: Long,
        uploadContent: MutableList<FolderContent.Data>,
    ): Pair<ArrayList<NameCollision>, MutableList<FolderContent.Data>> {
        val parent = getParentOrRootNode(parentHandle)
            ?: throw MegaNodeException.ParentDoesNotExistException()
        if (uploadContent.isEmpty()) throw EmptyFolderException()

        val collisions = uploadContent.mapNotNull { item ->
            runCatching {
                val name = item.name ?: return@mapNotNull null
                val handle = checkAsync(name = name, parent)
                NameCollision.Upload.getUploadCollision(
                    handle,
                    item,
                    parentHandle,
                )
            }.getOrNull()
        }
        return Pair(ArrayList(collisions), uploadContent)
    }


    /**
     * Checks a list of attached nodes in a chat conversation in order to know which names already
     * exist on the provided parent node.
     *
     * @param messageIds    Array of message identifiers.
     * @param chatId        Chat identifier.
     * @param parentHandle  Parent handle node in which to look.
     * @return Single<Pair<ArrayList<NameCollision>, List<MegaNode>>> containing:
     *  - First:    List of [NameCollision] with name collisions.
     *  - Second:   List of [MegaNode] without name collision.
     */
    fun checkMessagesToImport(
        messageIds: LongArray,
        chatId: Long,
        parentHandle: Long,
    ): Single<Pair<ArrayList<NameCollision>, List<MegaNode>>> =
        Single.create { emitter ->
            val parentNode = getNodeUseCase.get(parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            if (messageIds.isEmpty()) {
                emitter.onError(MessageDoesNotExistException())
                return@create
            }

            val collisions = ArrayList<NameCollision>()
            val nodesWithoutCollision = mutableListOf<MegaNode>()

            messageIds.forEach { messageId ->
                getChatMessageUseCase.getChatNodes(chatId, messageId).blockingSubscribeBy(
                    onError = { error -> Timber.e(error, "Error getting chat node.") },
                    onSuccess = { nodes ->
                        nodes.forEach { node ->
                            check(node.name, parentNode).blockingSubscribeBy(
                                onError = { error ->
                                    when (error) {
                                        is MegaNodeException.ChildDoesNotExistsException -> {
                                            nodesWithoutCollision.add(node)
                                        }

                                        else -> {
                                            emitter.onError(error)
                                        }
                                    }
                                },
                                onSuccess = { handle ->
                                    collisions.add(
                                        NameCollision.Import.getImportCollision(
                                            handle,
                                            chatId,
                                            messageId,
                                            node,
                                            parentHandle
                                        )
                                    )
                                }
                            )
                        }
                    }
                )
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(Pair(collisions, nodesWithoutCollision))
            }
        }
}