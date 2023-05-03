package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the handle by the default path string
 */
class GetDefaultNodeHandleUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * invoke
     *
     * @param defaultFolderName
     * @return [Long]
     */
    suspend operator fun invoke(defaultFolderName: String): Long {
        return nodeRepository.getDefaultNodeHandle(defaultFolderName)?.longValue
            ?: nodeRepository.getInvalidHandle()
    }
}
