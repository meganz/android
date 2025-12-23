package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.recentactions.NodeInfoForRecentActions
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import javax.inject.Inject

/**
 * Mapper to convert a list of [RecentActionBucketUnTyped] into [RecentActionBucket]
 *
 * This mapper handles all the transformations needed to convert untyped buckets
 * to fully typed buckets with all metadata (user names, shares type, key verification, etc.)
 */
class TypedRecentActionBucketMapper @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val addNodeType: AddNodeType,
    private val areCredentialsVerifiedUseCase: AreCredentialsVerifiedUseCase,
    private val nodeRepository: NodeRepository,
) {

    /**
     * Map a list of untyped buckets to typed buckets
     *
     * @param buckets The list of untyped buckets to map
     * @param visibleContacts Map of user emails to user names
     * @param currentUserEmail The current user's email
     * @return List of enriched [RecentActionBucket], filtered to exclude buckets with no valid nodes
     */
    suspend operator fun invoke(
        buckets: List<RecentActionBucketUnTyped>,
        visibleContacts: Map<String, String?>,
        currentUserEmail: String?,
    ): List<RecentActionBucket> {
        val verifiedCredentialsCache = mutableMapOf<String, Boolean>()
        val sharesTypeCache = mutableMapOf<Long, RecentActionsSharesType>()
        val mutex = Mutex()

        return buckets.mapNotNull { bucket ->
            // Process nodes to typed nodes
            val typedNodes = bucket.nodes.map { node ->
                addNodeType(node)
            }.filterIsInstance<TypedFileNode>()

            // Return null if no valid typed nodes found
            if (typedNodes.isEmpty()) {
                return@mapNotNull null
            }

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
            val isNodeKeyDecrypted =
                bucket.nodes.firstOrNull()?.isNodeKeyDecrypted == true
            val isNodeKeyVerified = isNodeKeyDecrypted || currentUserIsOwner || mutex.withLock {
                verifiedCredentialsCache.getOrPut(bucket.userEmail) {
                    areCredentialsVerified(bucket.userEmail)
                }
            }

            RecentActionBucket(
                identifier = bucket.identifier,
                timestamp = bucket.timestamp,
                dateTimestamp = bucket.dateTimestamp,
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
                isNodeKeyDecrypted = isNodeKeyDecrypted
            )
        }
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

