package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import javax.inject.Inject

/**
 * Get a list of recent actions
 */
class GetRecentActionsUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val addNodeType: AddNodeType,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
) {

    /**
     * Get a list of recent actions
     *
     * @return a list of recent actions
     */
    suspend operator fun invoke(): List<RecentActionBucket> = coroutineScope {
        val visibleContactsDeferred = async { getVisibleContactsUseCase() }
        val recentActionsDeferred = async { recentActionsRepository.getRecentActions() }
        val currentUserEmailDeferred = async { getAccountDetailsUseCase(false).email }

        val visibleContacts = visibleContactsDeferred.await()
        val currentUserEmail = currentUserEmailDeferred.await()

        // For caching
        val verifiedCredentialsCache = mutableMapOf<String, Boolean>()
        val sharesTypeCache = mutableMapOf<Long, RecentActionsSharesType>()

        val semaphore = Semaphore(10)
        val mutex = Mutex()

        recentActionsDeferred.await()
            .filter { it.nodes.isNotEmpty() }
            .map { bucket ->
                async(coroutineDispatcher) {
                    semaphore.withPermit {
                        val typedNodes = bucket.nodes.map { node ->
                            addNodeType(node)
                        }.filterIsInstance<TypedFileNode>()
                        val userName =
                            visibleContacts.find {
                                bucket.userEmail == it.email
                            }?.contactData?.fullName.orEmpty()
                        val currentUserIsOwner = currentUserEmail == bucket.userEmail
                        val parentNode = getNodeByIdUseCase(bucket.parentNodeId)
                        val sharesType = if (parentNode == null) {
                            RecentActionsSharesType.NONE
                        } else {
                            mutex.withLock {
                                sharesTypeCache.getOrPut(parentNode.id.longValue) {
                                    getParentSharesType(parentNode)
                                }
                            }
                        }
                        val isNodeKeyVerified =
                            bucket.nodes.firstOrNull()?.isNodeKeyDecrypted == true ||
                                    currentUserIsOwner ||
                                    mutex.withLock {
                                        verifiedCredentialsCache.getOrPut(bucket.userEmail) {
                                            areCredentialsVerified(bucket.userEmail)
                                        }
                                    }

                        RecentActionBucket(
                            timestamp = bucket.timestamp,
                            userEmail = bucket.userEmail,
                            parentNodeId = bucket.parentNodeId,
                            isUpdate = bucket.isUpdate,
                            isMedia = bucket.isMedia,
                            nodes = typedNodes,
                            userName = userName,
                            parentFolderName = parentNode?.name.orEmpty(),
                            parentFolderSharesType = sharesType,
                            currentUserIsOwner = currentUserIsOwner,
                            isKeyVerified = isNodeKeyVerified,
                        )
                    }
                }
            }
            .awaitAll()
            .filter { it.nodes.isNotEmpty() } // Filter out again as filterIsInstance inside map may return empty list
    }

    private suspend fun areCredentialsVerified(
        userEmail: String,
    ) = runCatching {
        areCredentialsVerifiedUseCase(userEmail)
    }.getOrDefault(false)

    /**
     * Retrieve the parent folder shares type of a node
     *
     * @param node
     * @return the shares type
     */
    private suspend fun getParentSharesType(node: TypedNode?): RecentActionsSharesType {
        return if (node is FolderNode) {
            when {
                node.isIncomingShare -> RecentActionsSharesType.INCOMING_SHARES
                node.isShared -> RecentActionsSharesType.OUTGOING_SHARES
                node.isPendingShare -> RecentActionsSharesType.PENDING_OUTGOING_SHARES
                else -> {
                    val parentNode = getNodeByIdUseCase(node.parentId)
                    getParentSharesType(parentNode)
                }
            }
        } else {
            RecentActionsSharesType.NONE
        }
    }
}
