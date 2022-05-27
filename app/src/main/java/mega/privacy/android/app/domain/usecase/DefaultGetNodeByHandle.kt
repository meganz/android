package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FilesRepository
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Default get the node corresponding to a handle
 *
 *  @property filesRepository
 */
class DefaultGetNodeByHandle @Inject constructor(
    private val filesRepository: FilesRepository
) : GetNodeByHandle {

    override fun invoke(handle: Long): MegaNode = filesRepository.getNodeByHandle(handle)
}