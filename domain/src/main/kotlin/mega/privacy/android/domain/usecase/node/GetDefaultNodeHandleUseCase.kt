package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the handle by the default path e.g. "Camera Uploads" string in the cloud folder and select
 * the folder only if the folder is not in the rubbish bin
 */
class GetDefaultNodeHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val isNodeInRubbishBinUseCase: IsNodeInRubbishBinUseCase,
) {

    /**
     * invoke
     *
     * @param defaultFolderName
     * @return [Long]
     */
    suspend operator fun invoke(defaultFolderName: String): Long {
        return nodeRepository.getDefaultNodeHandle(defaultFolderName)
            ?.takeIf { isNodeInRubbishBinUseCase(it).not() }?.longValue
            ?: nodeRepository.getInvalidHandle()
    }
}
