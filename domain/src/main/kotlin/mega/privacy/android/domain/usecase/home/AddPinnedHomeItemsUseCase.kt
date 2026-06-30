package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.entity.home.PinnedHomeItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Pin the given nodes to the Home screen; unresolvable nodes are skipped.
 */
class AddPinnedHomeItemsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    suspend operator fun invoke(nodeIds: List<NodeId>) {
        val items = nodeIds.mapNotNull { nodeId ->
            val node = getNodeByIdUseCase(nodeId) ?: return@mapNotNull null
            PinnedHomeItem(
                nodeId = nodeId,
                name = node.name,
                isFolder = node is TypedFolderNode,
            )
        }
        if (items.isNotEmpty()) {
            settingsRepository.addPinnedHomeItems(items)
        }
    }
}
