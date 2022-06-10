package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import javax.inject.Inject

/**
 * Default root node exists
 *
 * @property filesRepository
 */
class DefaultRootNodeExists @Inject constructor(private val filesRepository: FilesRepository) :
    RootNodeExists {
    override suspend fun invoke(): Boolean {
        return filesRepository.getRootNode() != null
    }
}