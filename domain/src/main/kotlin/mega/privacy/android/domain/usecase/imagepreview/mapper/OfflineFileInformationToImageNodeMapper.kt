package mega.privacy.android.domain.usecase.imagepreview.mapper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import javax.inject.Inject


/**
 * Mapper to map OfflineFileInformation to ImageNode
 */
class OfflineFileInformationToImageNodeMapper @Inject constructor() {
    /**
     * Invoke
     * @param offlineFileInformation [OfflineFileInformation]
     */
    operator fun invoke(
        offlineFileInformation: OfflineFileInformation,
        filterSvg: Boolean,
    ): ImageNode? {
        if (offlineFileInformation.isFolder) return null
        val fileType = offlineFileInformation.fileTypeInfo ?: return null
        val isImageType = fileType is ImageFileTypeInfo
                && (fileType !is SvgFileTypeInfo || !filterSvg)
        if (!(isImageType || fileType is VideoFileTypeInfo)) return null

        val nodeId = NodeId(offlineFileInformation.handle.toLongOrNull() ?: return null)
        return object : ImageNode {
            override val id = nodeId
            override val name = offlineFileInformation.name
            override val size = offlineFileInformation.totalSize
            override val label = -1
            override val parentId = NodeId(-1L)
            override val base64Id = ""
            override val restoreId = null
            override val creationTime = offlineFileInformation.addedTime ?: -1L
            override val modificationTime = offlineFileInformation.addedTime ?: -1L
            override val thumbnailPath = null
            override val previewPath = null
            override val fullSizePath = offlineFileInformation.absolutePath
            override val type = fileType
            override val isFavourite = false
            override val isMarkedSensitive = false
            override val isSensitiveInherited = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val fingerprint = null
            override val originalFingerprint = null
            override val isNodeKeyDecrypted = true
            override val hasThumbnail = true
            override val hasPreview = false
            override val downloadThumbnail: suspend (String) -> String =
                { _ -> offlineFileInformation.thumbnail ?: "" }
            override val downloadPreview: suspend (String) -> String = { _ -> "" }
            override val downloadFullImage: (String, Boolean, () -> Unit) -> Flow<ImageProgress> =
                { _, _, _ -> flowOf() }
            override val latitude = -1.0
            override val longitude = -1.0
            override val serializedData = "offlineFile"
            override val isAvailableOffline: Boolean = true
            override val versionCount: Int = -1
            override val description: String? = null
            override val tags: List<String>? = null
        }
    }
}