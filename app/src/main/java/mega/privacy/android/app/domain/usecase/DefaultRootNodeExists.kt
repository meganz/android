package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.usecase.RootNodeExists
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