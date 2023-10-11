package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject

/**
 * Find the node by fingerprint or original fingerprint and check if it exists in the current parent folder
 * or not (except the rubbish bin folder)
 */
class FindNodeWithFingerprintInParentNodeUseCase @Inject constructor(
    private val getNodeFromCloudUseCase: GetNodeFromCloudUseCase,
    private val isNodeInRubbish: IsNodeInRubbish,
) {

    /**
     * Find the node by fingerprint or original fingerprint and check if it exists in the current parent folder
     * or not (except the rubbish bin folder)
     *
     * @param fingerprint the fingerprint of the original file
     * @param generatedFingerprint the computed fingerprint of the temporary file
     * @param parentNodeId parent node id to look for the node
     * @return a Pair<Boolean?, NodeId?>
     *         The first element will return true if it exists in the parent folder given in parameter, false otherwise, null if in rubbish bin
     *         The second element will return the node retrieved, null if cannot be retrieved
     */
    suspend operator fun invoke(
        fingerprint: String,
        generatedFingerprint: String?,
        parentNodeId: NodeId,
    ): Pair<Boolean?, NodeId?> {
        val nodeExists = getNodeFromCloudUseCase(
            fingerprint,
            generatedFingerprint,
            parentNodeId,
        )

        return nodeExists?.let { node ->
            val isNodeInRubbish = isNodeInRubbish(node.id.longValue)
            val isNodeInParentFolder =
                node.parentId.longValue == parentNodeId.longValue

            when {
                isNodeInParentFolder -> Pair(true, node.id)

                !isNodeInRubbish && !isNodeInParentFolder -> Pair(false, node.id)

                else -> Pair(null, node.id)
            }
        } ?: run {
            Pair(false, null)
        }
    }
}

