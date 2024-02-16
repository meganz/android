package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.data.mapper.MimeTypeMapper
import mega.privacy.android.data.mapper.getFileTypeInfoForExtension
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File
import javax.inject.Inject

/**
 * The mapper class to convert from File to ImageNode
 */
internal class ImageNodeFileMapper @Inject constructor(
    private val mimeTypeMapper: MimeTypeMapper,
) {
    operator fun invoke(file: File) = object : ImageNode {
        override val id = NodeId(file.hashCode().toLong())
        override val name = file.name
        override val size = file.length()
        override val label = -1
        override val parentId = NodeId(-1)
        override val base64Id = ""
        override val restoreId = NodeId(-1)
        override val creationTime = -1L
        override val modificationTime = file.lastModified()
        override val thumbnailPath = file.absolutePath
        override val previewPath = file.absolutePath
        override val fullSizePath = file.absolutePath
        override val type = getFileTypeInfoForExtension(
            mimeType = mimeTypeMapper(file.extension),
            extension = file.extension,
            duration = 0,
        )
        override val isFavourite = false
        override val exportedData = null
        override val isTakenDown = false
        override val isIncomingShare = false
        override val fingerprint = null
        override val originalFingerprint = null
        override val isNodeKeyDecrypted = false
        override val hasThumbnail = true
        override val hasPreview = true
        override val downloadThumbnail: suspend (String) -> String = { _ -> "" }
        override val downloadPreview: suspend (String) -> String = { _ -> "" }
        override val downloadFullImage: (String, Boolean, () -> Unit) -> Flow<ImageProgress> =
            { _, _, _ -> flowOf() }
        override val latitude = -1.0
        override val longitude = -1.0
        override val serializedData = "localFile"
        override val isAvailableOffline: Boolean = false
        override val versionCount: Int = -1
    }
}