package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Offline folder info by parent Id, e.g. number of files and folder
 *
 */
class GetOfflineFolderInformationUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * invoke
     * @param parentId [Int]
     */
    suspend operator fun invoke(parentId: Int) =
        nodeRepository.getOfflineFolderInfo(parentId) ?: OfflineFolderInfo(
            numFolders = 0,
            numFiles = 0
        )

}