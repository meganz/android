package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import javax.inject.Inject

/**
 * Get OfflineFileInformation by OfflineNodeInformation
 *
 */
class GetOfflineFileInformationUseCase @Inject constructor(
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getThumbnailUseCase: GetThumbnailUseCase,
    private val getOfflineFolderInformationUseCase: GetOfflineFolderInformationUseCase,
    private val getOfflineFileTotalSizeUseCase: GetOfflineFileTotalSizeUseCase,
) {
    /**
     * Invoke
     * @param offlineNodeInformation [OfflineNodeInformation]
     */
    suspend operator fun invoke(offlineNodeInformation: OfflineNodeInformation): OfflineFileInformation {
        val nodeHandle = offlineNodeInformation.handle.toLongOrNull() ?: -1L
        val offlineFile = getOfflineFileUseCase(offlineNodeInformation)
        val totalSize = getOfflineFileTotalSizeUseCase(offlineFile)
        val folderInfo = getFolderInfoOrNull(offlineNodeInformation)
        val thumbnail = getThumbnailPathOrNull(offlineNodeInformation.isFolder, nodeHandle)
        val addedTime = offlineNodeInformation.lastModifiedTime?.div(1000L)

        return OfflineFileInformation(
            id = offlineNodeInformation.id,
            handle = nodeHandle,
            parentId = offlineNodeInformation.parentId,
            name = offlineNodeInformation.name,
            totalSize = totalSize,
            isFolder = offlineNodeInformation.isFolder,
            folderInfo = folderInfo,
            addedTime = addedTime,
            thumbnail = thumbnail,
        )
    }

    private suspend fun getThumbnailPathOrNull(
        isFolder: Boolean,
        handle: Long,
    ): String? {
        if (isFolder) return null
        return getThumbnailUseCase(handle)
            ?.takeIf { it.exists() }
            ?.toURI()
            ?.toString()
    }

    private suspend fun getFolderInfoOrNull(
        offlineNodeInfo: OfflineNodeInformation,
    ) = offlineNodeInfo.takeIf { it.isFolder }?.let {
        getOfflineFolderInformationUseCase(it.id)
    }
}