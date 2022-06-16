package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default [HasChildren] implementation.
 *
 * @param filesRepository [FilesRepository]
 */
class DefaultHasChildren @Inject constructor(
    private val filesRepository: FilesRepository,
) : HasChildren {

    override suspend fun invoke(node: MegaNode): Boolean = filesRepository.hasChildren(node)
}