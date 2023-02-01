package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.usecase.RootNodeExists
import javax.inject.Inject

/**
 * Default root node exists
 *
 * @property megaNodeRepository
 */
class DefaultRootNodeExists @Inject constructor(private val megaNodeRepository: MegaNodeRepository) :
    RootNodeExists {
    override suspend fun invoke(): Boolean {
        return megaNodeRepository.getRootNode() != null
    }
}