package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.thumbnailpreview.ThumbnailPreviewRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.domain.usecase.file.IsImageFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get OfflineFileInformation by OfflineNodeInformation
 *
 * The returned `thumbnail` is only populated from local sources (the offline file itself
 * when `useOriginalImageAsThumbnail` is set, or a previously cached thumbnail on disk).
 * This use case never reaches the network — UIs should fall back to fetching by node handle
 * via Coil's MegaThumbnailFetcher when `thumbnail` is null.
 */
class GetOfflineFileInformationUseCase @Inject constructor(
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val thumbnailPreviewRepository: ThumbnailPreviewRepository,
    private val getOfflineFolderInformationUseCase: GetOfflineFolderInformationUseCase,
    private val getOfflineFileTotalSizeUseCase: GetOfflineFileTotalSizeUseCase,
    private val isImageFileUseCase: IsImageFileUseCase,
    private val getFileTypeInfoUseCase: GetFileTypeInfoUseCase,
) {
    /**
     * Invoke
     * @param offlineNodeInformation [OfflineNodeInformation]
     * @param useOriginalImageAsThumbnail [Boolean] use original image file as thumbnail
     */
    suspend operator fun invoke(
        offlineNodeInformation: OfflineNodeInformation,
        useOriginalImageAsThumbnail: Boolean = false,
    ): OfflineFileInformation {
        val nodeHandle = offlineNodeInformation.handle.toLongOrNull() ?: -1L
        val offlineFile = getOfflineFileUseCase(offlineNodeInformation)
        val totalSize = getOfflineFileTotalSizeUseCase(offlineFile)
        val folderInfo = getFolderInfoOrNull(offlineNodeInformation)
        val fileTypeInfo = takeIf { !offlineNodeInformation.isFolder }?.let {
            getFileTypeInfoUseCase(offlineFile)
        }
        val thumbnail = getThumbnailPathOrNull(
            offlineFile = offlineFile,
            useOriginalImageAsThumbnail = useOriginalImageAsThumbnail,
            isFolder = offlineNodeInformation.isFolder,
            handle = nodeHandle
        )

        return OfflineFileInformation(
            nodeInfo = offlineNodeInformation,
            totalSize = totalSize,
            folderInfo = folderInfo,
            thumbnail = thumbnail,
            fileTypeInfo = fileTypeInfo,
            absolutePath = offlineFile.absolutePath
        )
    }

    private suspend fun getThumbnailPathOrNull(
        offlineFile: File,
        useOriginalImageAsThumbnail: Boolean,
        isFolder: Boolean,
        handle: Long,
    ): String? = when {
        isFolder -> null
        useOriginalImageAsThumbnail && isImageFileUseCase(UriPath.fromFile(offlineFile)) -> offlineFile
        else -> thumbnailPreviewRepository.getThumbnailFromLocal(handle)
    }?.takeIf { it.exists() }?.toURI()?.toString()

    private suspend fun getFolderInfoOrNull(
        offlineNodeInfo: OfflineNodeInformation,
    ) = offlineNodeInfo.takeIf { it.isFolder }?.let {
        getOfflineFolderInformationUseCase(it.id)
    }
}