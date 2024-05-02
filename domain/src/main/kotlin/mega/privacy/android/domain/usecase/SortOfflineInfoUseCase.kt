package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import javax.inject.Inject

/**
 * Use case to sort offline information
 */
class SortOfflineInfoUseCase @Inject constructor() {

    /**
     * invoke
     */
    operator fun invoke(offlineInfoList: List<OfflineNodeInformation>): List<OfflineNodeInformation> {
        val folderItems = offlineInfoList.filter {
            it.isFolder
        }.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER,
                OfflineNodeInformation::name
            )
        )
        val fileItems = offlineInfoList.filter {
            it.isFolder.not()
        }.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER,
                OfflineNodeInformation::name
            )
        )
        return folderItems + fileItems
    }
}