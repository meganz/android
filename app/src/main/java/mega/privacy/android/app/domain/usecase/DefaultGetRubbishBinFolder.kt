package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get the rubbish bin node
 *
 *  @property filesRepository
 */
class DefaultGetRubbishBinFolder @Inject constructor(
    private val filesRepository: FilesRepository
) : GetRubbishBinFolder {

    override suspend fun invoke(): MegaNode? = filesRepository.getRubbishBinNode()
}