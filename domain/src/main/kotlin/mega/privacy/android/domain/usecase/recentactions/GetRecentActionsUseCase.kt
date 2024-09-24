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
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import javax.inject.Inject

/**
 * Get a list of recent actions
 */
class GetRecentActionsUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val addNodeType: AddNodeType,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val nodeRepository: NodeRepository,
    private val contactsRepository: ContactsRepository,
    @IoDispatcher private val coroutineDispatcher: CoroutineDispatcher,
) {

    /**
     * Get a list of recent actions
     *
     * @param excludeSensitives Exclude sensitive nodes
     * @return a list of recent actions
     */
    suspend operator fun invoke(
        excludeSensitives: Boolean,
    ): List<RecentActionBucket> = coroutineScope {
        val visibleContactsDeferred = async { contactsRepository.getAllContactsName() }
        val recentActionsDeferred = async {
            recentActionsRepository.getRecentActions(
                excludeSensitives = excludeSensitives,
            )
        }
        val currentUserEmailDeferred = async { getCurrentUserEmail(false) }

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
                        val userName = visibleContacts[bucket.userEmail] ?: bucket.userEmail
                        val currentUserIsOwner = currentUserEmail == bucket.userEmail
                        val parentNodeInfo =
                            recentActionsRepository.getNodeInfo(bucket.parentNodeId)
                        val nodeAccessLevel = nodeRepository.getNodeAccessPermission(
                            nodeId = typedNodes.first().id
                        )
                        val sharesType = if (parentNodeInfo == null) {
                            RecentActionsSharesType.NONE
                        } else if (currentUserIsOwner) {
                            if (nodeAccessLevel == AccessPermission.OWNER) {
                                mutex.withLock {
                                    sharesTypeCache.getOrPut(parentNodeInfo.id.longValue) {
                                        getParentSharesType(parentNodeInfo)
                                    }
                                }
                            } else {
                                RecentActionsSharesType.INCOMING_SHARES
                            }
                        } else {
                            if (nodeAccessLevel == AccessPermission.OWNER) {
                                RecentActionsSharesType.OUTGOING_SHARES
                            } else {
                                RecentActionsSharesType.INCOMING_SHARES
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
                            parentFolderName = parentNodeInfo?.name.orEmpty(),
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
    private suspend fun getParentSharesType(node: NodeInfoForRecentActions?): RecentActionsSharesType {
        return if (node?.isFolder == true) {
            when {
                node.isIncomingShare -> RecentActionsSharesType.INCOMING_SHARES
                node.isOutgoingShare -> RecentActionsSharesType.OUTGOING_SHARES
                node.isPendingShare -> RecentActionsSharesType.PENDING_OUTGOING_SHARES
                else -> {
                    getParentSharesType(recentActionsRepository.getNodeInfo(node.parentId))
                }
            }
        } else {
            RecentActionsSharesType.NONE
        }
    }
}
