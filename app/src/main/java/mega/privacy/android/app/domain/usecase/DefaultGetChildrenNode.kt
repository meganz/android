package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get children nodes of a parent node
 *
 *  @property filesRepository
 */
class DefaultGetChildrenNode @Inject constructor(
    private val filesRepository: FilesRepository
) : GetChildrenNode {

    override suspend fun invoke(parent: MegaNode, order: Int?): List<MegaNode> =
        filesRepository.getChildrenNode(parent, order)
}