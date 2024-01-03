package mega.privacy.android.feature.sync.ui.megapicker

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode

internal class SampleNodeDataProvider {

    internal companion object {
        private val nodeUIItem1 = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(1L)
            override val name = "My important files"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val label = 1
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount: Int = 0
        }

        private val nodeUIItem2 = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(2L)
            override val name = "Less important files"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val label = 1
            override val isFavourite = true
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount: Int = 0
        }

        private val nodeUIItem3 = object : TypedFolderNode {
            override val isInRubbishBin = false
            override val isShared = false
            override val isPendingShare = false
            override val device = null
            override val childFolderCount = 1
            override val childFileCount = 1
            override val fetchChildren: suspend (SortOrder) -> List<UnTypedNode> = { emptyList() }
            override val type = FolderType.Default
            override val id = NodeId(3L)
            override val name = "some file"
            override val parentId = NodeId(2L)
            override val base64Id = "1L"
            override val label = 1
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount: Int = 0
        }

        private val nodeUIItem4 = object : TypedFileNode {
            override val id = NodeId(4L)
            override val name = "stuff"
            override val parentId = NodeId(2L)
            override val base64Id = "14L"
            override val label = 1
            override val isFavourite = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val size: Long = 8989
            override val modificationTime: Long = 12313312134
            override val type: FileTypeInfo = PdfFileTypeInfo
            override val thumbnailPath =
                "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"
            override val previewPath =
                "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"
            override val fullSizePath: String =
                "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"
            override val fingerprint: String = "finger_lickin_good"
            override val originalFingerprint: String = "originals"
            override val hasThumbnail: Boolean = true
            override val hasPreview: Boolean = true
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount: Int = 0
        }

        val values: List<TypedNode> =
            listOf(
                nodeUIItem1, nodeUIItem2, nodeUIItem3, nodeUIItem4
            )
    }
}