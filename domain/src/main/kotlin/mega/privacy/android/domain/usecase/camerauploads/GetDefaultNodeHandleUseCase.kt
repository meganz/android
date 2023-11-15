package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import javax.inject.Inject

/**
 * Get the handle by the default path e.g. "Camera Uploads" string in the cloud folder and select
 * the folder only if the folder is not in the rubbish bin
 */
class GetDefaultNodeHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val isNodeInRubbish: IsNodeInRubbish,
) {

    /**
     * invoke
     *
     * @param defaultFolderName
     * @return [Long]
     */
    suspend operator fun invoke(defaultFolderName: String): Long {
        return nodeRepository.getDefaultNodeHandle(defaultFolderName)
            ?.takeIf { isNodeInRubbish(it.longValue).not() }?.longValue
            ?: nodeRepository.getInvalidHandle()
    }
}
