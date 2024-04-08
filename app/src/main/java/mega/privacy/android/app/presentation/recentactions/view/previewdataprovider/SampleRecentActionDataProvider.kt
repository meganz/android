package mega.privacy.android.app.presentation.recentactions.view.previewdataprovider

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.recentactions.model.RecentActionBucketUiEntity
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode

internal class SampleRecentActionDataProvider :
    PreviewParameterProvider<List<RecentActionBucketUiEntity>> {

    data class SampleTypedFileNode(
        private val fileNode: FileNode,
    ) : TypedFileNode, FileNode by fileNode

    private val fileNode1 = SampleTypedFileNode(
        fileNode = object : TypedFileNode {
            override val id = NodeId(2L)
            override val name = "File Name"
            override val parentId = NodeId(1L)
            override val base64Id = "1L"
            override val restoreId = null
            override val label = 1
            override val isFavourite = false
            override val isMarkedSensitive = false
            override val isSensitiveInherited = false
            override val exportedData = null
            override val isTakenDown = false
            override val isIncomingShare = false
            override val isNodeKeyDecrypted = false
            override val creationTime = System.currentTimeMillis()
            override val serializedData = null
            override val isAvailableOffline = false
            override val versionCount = 0
            override val size = 1000L
            override val modificationTime = System.currentTimeMillis()
            override val type = StaticImageFileTypeInfo("image/jpeg", "jpg")
            override val thumbnailPath = "/thumbnail"
            override val previewPath = "/preview"
            override val fullSizePath = "/abc/xyz"
            override val fingerprint = "xyz"
            override val originalFingerprint = "abc"
            override val hasThumbnail = false
            override val hasPreview = false
        }
    )

    private val recentBucket1 = RecentActionBucket(
        timestamp = System.currentTimeMillis() / 1000,
        userEmail = "user1@mail.com",
        parentNodeId = NodeId(1L),
        isUpdate = false,
        isMedia = false,
        nodes = listOf(fileNode1),
        userName = "User1",
        parentFolderName = "Folder1",
        parentFolderSharesType = RecentActionsSharesType.NONE,
        currentUserIsOwner = true,
        isKeyVerified = true
    )
    override val values: Sequence<List<RecentActionBucketUiEntity>> = sequenceOf(
        listOf(
            RecentActionBucketUiEntity(
                firstLineText = "First line text",
                updatedByText = "[A]Updated by[/A] [B]John Doe[/B]",
                isFavourite = true,
                labelColor = R.color.red_200,
                shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
                time = "12:00 PM",
                date = "Today",
                parentFolderName = "Folder Name",
                actionIcon = R.drawable.ic_recents_up,
                icon = IconPackR.drawable.ic_generic_medium_solid,
                showMenuButton = true,
                bucket = recentBucket1
            ),
            RecentActionBucketUiEntity(
                firstLineText = "First line text",
                updatedByText = null,
                isFavourite = true,
                labelColor = null,
                shareIcon = IconPackR.drawable.ic_folder_incoming_medium_solid,
                date = "Today",
                time = "10:00 PM",
                parentFolderName = "Folder Name",
                actionIcon = R.drawable.ic_recents_up,
                icon = IconPackR.drawable.ic_generic_medium_solid,
                showMenuButton = true,
                bucket = recentBucket1
            ),
            RecentActionBucketUiEntity(
                firstLineText = "2 Videos and 2 Photos",
                updatedByText = null,
                isFavourite = false,
                labelColor = null,
                shareIcon = null,
                date = "24th March, 2024",
                time = "08:00 PM",
                parentFolderName = "Videos",
                actionIcon = R.drawable.ic_recents_up,
                icon = IconPackR.drawable.ic_image_stack_medium_solid,
                showMenuButton = false,
                bucket = recentBucket1
            )
        )
    )
}
