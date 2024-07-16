package mega.privacy.android.app.namecollision.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.coroutines.withContext
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.data.gateway.api.MegaApiGateway
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
 * @property getChatMessageUseCase  Required for getting chat [MegaNode]s.
 */
@OptIn(ExperimentalContracts::class)
@Deprecated("Use CheckNodesNameCollisionUseCase")
class CheckNameCollisionUseCase @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    private suspend fun getParentOrRootNode(parentHandle: Long) =
        if (parentHandle == INVALID_HANDLE) megaApiGateway.getRootNode() else megaApiGateway.getMegaNodeByHandle(
            parentHandle
        )

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

    suspend fun checkAsync(name: String, parent: MegaNode?): Long {
        contract { returns() implies (parent != null) }
        if (parent == null) {
            throw MegaNodeException.ParentDoesNotExistException()
        }

        return withContext(ioDispatcher) { megaApiGateway.getChildNode(parent, name)?.handle }
            ?: throw MegaNodeException.ChildDoesNotExistsException()
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
}