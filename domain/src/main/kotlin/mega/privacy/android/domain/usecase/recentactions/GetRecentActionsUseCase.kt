package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
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
) {

    /**
     * Get a list of recent actions
     *
     * @return a list of recent actions
     */
    suspend operator fun invoke(): List<RecentActionBucket> = coroutineScope {
        val visibleContactsDeferred = async { getVisibleContactsUseCase() }
        val recentActionsDeferred = async { recentActionsRepository.getRecentActions() }
        val visibleContacts = visibleContactsDeferred.await()
        recentActionsDeferred.await()
            .filter { it.nodes.isNotEmpty() }
            .map { bucket ->
                async {
                    val typedNodesDeferred = async {
                        bucket.nodes.map { node ->
                            addNodeType(node)
                        }.filterIsInstance<TypedFileNode>()
                    }
                    val userNameDeferred = async {
                        visibleContacts.find {
                            bucket.userEmail == it.email
                        }?.contactData?.fullName.orEmpty()
                    }
                    val currentUserIsOwnerDeferred = async { isCurrentUserOwner(bucket.userEmail) }
                    val parentNodeDeferred =
                        async { getNodeByIdUseCase(bucket.parentNodeId) }
                    val parentNode = parentNodeDeferred.await()
                    val sharesTypeDeferred = async { getParentSharesType(parentNode) }
                    val isNodeKeyVerified = bucket.nodes.firstOrNull()?.isNodeKeyDecrypted == true
                            || areCredentialsVerified(bucket.userEmail)
                    RecentActionBucket(
                        timestamp = bucket.timestamp,
                        userEmail = bucket.userEmail,
                        parentNodeId = bucket.parentNodeId,
                        isUpdate = bucket.isUpdate,
                        isMedia = bucket.isMedia,
                        nodes = typedNodesDeferred.await(),
                        userName = userNameDeferred.await(),
                        parentFolderName = parentNode?.name.orEmpty(),
                        parentFolderSharesType = sharesTypeDeferred.await(),
                        currentUserIsOwner = currentUserIsOwnerDeferred.await(),
                        isKeyVerified = isNodeKeyVerified,
                    )
                }
            }
            .awaitAll()
            .filter { it.nodes.isNotEmpty() } // Filter out again as filterIsInstance inside map may return empty list
    }

    private suspend fun isCurrentUserOwner(
        userEmail: String,
    ) = runCatching { getAccountDetailsUseCase(false).email == userEmail }
        .getOrDefault(false)

    private suspend fun areCredentialsVerified(
        userEmail: String,
    ) = runCatching { areCredentialsVerifiedUseCase(userEmail) }
        .getOrDefault(false)

    /**
     * Retrieve the parent folder shares type of a node
     *
     * @param node
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
