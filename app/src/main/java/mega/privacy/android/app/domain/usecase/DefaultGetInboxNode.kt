package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default [GetInboxNode] implementation.
 *
 * @param filesRepository [FilesRepository]
 */
class DefaultGetInboxNode @Inject constructor(
    private val filesRepository: FilesRepository,
) : GetInboxNode {

    override suspend fun invoke(): MegaNode? = filesRepository.getInboxNode()
}