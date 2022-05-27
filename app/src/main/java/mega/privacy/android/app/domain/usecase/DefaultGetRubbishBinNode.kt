package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get the rubbish bin node
 *
 *  @property filesRepository
 */
class DefaultGetRubbishBinNode @Inject constructor(
    private val filesRepository: FilesRepository
) : GetRubbishBinNode {

    override fun invoke(): MegaNode? = filesRepository.getRubbishBinNode()
}