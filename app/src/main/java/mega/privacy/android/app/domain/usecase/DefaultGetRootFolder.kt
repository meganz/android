package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get the root node
 *
 *  @property filesRepository
 */
class DefaultGetRootFolder @Inject constructor(
    private val filesRepository: FilesRepository
) : GetRootFolder {

    override fun invoke(): MegaNode? = filesRepository.getRootNode()
}