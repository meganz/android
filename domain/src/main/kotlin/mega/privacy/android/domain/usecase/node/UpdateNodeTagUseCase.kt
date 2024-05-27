package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to update the tag of a node.
 *
 * @property nodeRepository Repository to provide the necessary data.
 */
class UpdateNodeTagUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * Invoke the use case.
     *
     * @param nodeHandle Node handle of the node to update the tag.
     * @param oldTag     Old tag to remove.
     * @param newTag     New tag to add.
     */
    suspend operator fun invoke(nodeHandle: NodeId, oldTag: String? = null, newTag: String?) =
        when {
            newTag == null && oldTag != null -> {
                nodeRepository.removeNodeTag(nodeHandle, oldTag)
            }

            newTag != null && oldTag == null -> {
                nodeRepository.addNodeTag(nodeHandle, newTag)
            }

            newTag != null && oldTag != null -> {
                nodeRepository.updateNodeTag(nodeHandle, newTag, oldTag)
            }

            else -> {
                throw IllegalArgumentException("Both oldTag and newTag are null")
            }
        }
}