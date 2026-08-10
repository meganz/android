package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Refresh the stored name of a pinned Home item after its node is renamed.
 */
class UpdatePinnedHomeItemNameUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(nodeId: NodeId, name: String) {
        settingsRepository.updatePinnedHomeItemName(nodeId, name)
    }
}
