package mega.privacy.mobile.home.presentation.home.widget.recents.mapper

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.NodeLabel
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.home.R
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.mobile.home.presentation.home.widget.recents.model.RecentActionTitleText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsUiItemMapperTest {

    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private lateinit var underTest: RecentActionUiItemMapper

    @BeforeEach
    fun setUp() {
        // Setup default return for fileTypeIconMapper
        whenever(fileTypeIconMapper("txt")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        whenever(fileTypeIconMapper("pdf")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        whenever(fileTypeIconMapper("jpeg")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        whenever(fileTypeIconMapper("png")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        whenever(fileTypeIconMapper("mp4")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        whenever(fileTypeIconMapper("")).thenReturn(IconPackR.drawable.ic_generic_medium_solid)
        underTest = RecentActionUiItemMapper(fileTypeIconMapper)
    }

    @Test
    fun `test that single node is mapped correctly`() {
        val node = createMockFileNode(
            name = "testFile.txt",
            isFavourite = true,
            nodeLabel = NodeLabel.RED
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isMedia = false,
            isNodeKeyDecrypted = true,
            currentUserIsOwner = true
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.SingleNode::class.java)
        assertThat((result.title as RecentActionTitleText.SingleNode).nodeName).isEqualTo("testFile.txt")
        assertThat(result.isMediaBucket).isFalse()
        assertThat(result.isFavourite).isTrue()
        assertThat(result.nodeLabel).isEqualTo(NodeLabel.RED)
        assertThat(result.isUpdate).isFalse()
        assertThat(result.updatedByText).isNull()
        assertThat(result.userName).isNull()
        assertThat(result.shareIcon).isNull()
    }

    @Test
    fun `test that media bucket with images only is mapped correctly`() {
        val imageNode1 = createMockFileNode(
            name = "image1.jpg",
            mimeType = "image/jpeg"
        )
        val imageNode2 = createMockFileNode(
            name = "image2.png",
            mimeType = "image/png"
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(imageNode1, imageNode2),
            isMedia = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.MediaBucketImagesOnly::class.java)
        assertThat((result.title as RecentActionTitleText.MediaBucketImagesOnly).numImages).isEqualTo(
            2
        )
        assertThat(result.isMediaBucket).isTrue()
        assertThat(result.icon).isEqualTo(IconPackR.drawable.ic_image_stack_medium_solid)
        assertThat(result.isFavourite).isFalse()
        assertThat(result.nodeLabel).isNull()
    }

    @Test
    fun `test that media bucket with videos only is mapped correctly`() {
        val videoNode1 = createMockFileNode(
            name = "video1.mp4",
            mimeType = "video/mp4"
        )
        val videoNode2 = createMockFileNode(
            name = "video2.mov",
            mimeType = "video/quicktime"
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(videoNode1, videoNode2),
            isMedia = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.MediaBucketVideosOnly::class.java)
        assertThat((result.title as RecentActionTitleText.MediaBucketVideosOnly).numVideos).isEqualTo(
            2
        )
        assertThat(result.isMediaBucket).isTrue()
        assertThat(result.icon).isEqualTo(IconPackR.drawable.ic_image_stack_medium_solid)
    }

    @Test
    fun `test that media bucket with mixed images and videos is mapped correctly`() {
        val imageNode = createMockFileNode(
            name = "image.jpg",
            mimeType = "image/jpeg"
        )
        val videoNode = createMockFileNode(
            name = "video.mp4",
            mimeType = "video/mp4"
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(imageNode, videoNode),
            isMedia = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.MediaBucketMixed::class.java)
        val mixedTitle = result.title as RecentActionTitleText.MediaBucketMixed
        assertThat(mixedTitle.numImages).isEqualTo(1)
        assertThat(mixedTitle.numVideos).isEqualTo(1)
        assertThat(result.isMediaBucket).isTrue()
    }

    @Test
    fun `test that regular bucket is mapped correctly`() {
        val node1 = createMockFileNode(name = "file1.txt")
        val node2 = createMockFileNode(name = "file2.txt")
        val node3 = createMockFileNode(name = "file3.txt")
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2, node3),
            isMedia = false,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.RegularBucket::class.java)
        val regularTitle = result.title as RecentActionTitleText.RegularBucket
        assertThat(regularTitle.nodeName).isEqualTo("file1.txt")
        assertThat(regularTitle.additionalCount).isEqualTo(2)
        assertThat(result.isMediaBucket).isFalse()
    }

    @Test
    fun `test that undecrypted files are mapped correctly`() {
        val node = createMockFileNode(name = "encrypted.txt")
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = false
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.UndecryptedFiles::class.java)
        assertThat((result.title as RecentActionTitleText.UndecryptedFiles).count).isEqualTo(1)
        assertThat(result.parentFolderName).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result.parentFolderName as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.shared_items_verify_credentials_undecrypted_folder)
    }

    @Test
    fun `test that undecrypted files with multiple nodes shows correct count`() {
        val node1 = createMockFileNode(name = "encrypted1.txt")
        val node2 = createMockFileNode(name = "encrypted2.txt")
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2),
            isNodeKeyDecrypted = false
        )

        val result = underTest(bucket)

        assertThat(result.title).isInstanceOf(RecentActionTitleText.UndecryptedFiles::class.java)
        assertThat((result.title as RecentActionTitleText.UndecryptedFiles).count).isEqualTo(2)
    }

    @Test
    fun `test that parent folder name Cloud Drive is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderName = "Cloud Drive",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.parentFolderName).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result.parentFolderName as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.section_cloud_drive)
    }

    @Test
    fun `test that regular parent folder name is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderName = "My Folder",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.parentFolderName).isInstanceOf(LocalizedText.Literal::class.java)
        // Note: We only verify the type since text resolution requires @Composable context
        // The actual text value will be resolved in the Composable layer
    }

    @Test
    fun `test that incoming shares icon is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderSharesType = RecentActionsSharesType.INCOMING_SHARES,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.shareIcon).isEqualTo(IconPackR.drawable.ic_folder_users_small_solid)
    }

    @Test
    fun `test that outgoing shares icon is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderSharesType = RecentActionsSharesType.OUTGOING_SHARES,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.shareIcon).isEqualTo(IconPackR.drawable.ic_folder_users_small_solid)
    }

    @Test
    fun `test that pending outgoing shares icon is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderSharesType = RecentActionsSharesType.PENDING_OUTGOING_SHARES,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.shareIcon).isEqualTo(IconPackR.drawable.ic_folder_users_small_solid)
    }

    @Test
    fun `test that no share icon when shares type is NONE`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            parentFolderSharesType = RecentActionsSharesType.NONE,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.shareIcon).isNull()
    }

    @Test
    fun `test that updatedByText is set when current user is not owner and isUpdate is true`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            currentUserIsOwner = false,
            isUpdate = true,
            userName = "John Doe",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.updatedByText).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result.updatedByText as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.update_action_bucket)
        assertThat(result.userName).isEqualTo("John Doe")
    }

    @Test
    fun `test that updatedByText is set when current user is not owner and isUpdate is false`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            currentUserIsOwner = false,
            isUpdate = false,
            userName = "Jane Doe",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.updatedByText).isInstanceOf(LocalizedText.StringRes::class.java)
        assertThat((result.updatedByText as LocalizedText.StringRes).resId)
            .isEqualTo(R.string.create_action_bucket)
        assertThat(result.userName).isEqualTo("Jane Doe")
    }

    @Test
    fun `test that updatedByText is null when current user is owner`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            currentUserIsOwner = true,
            userName = "Owner",
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.updatedByText).isNull()
        assertThat(result.userName).isNull()
    }

    @Test
    fun `test that isUpdate flag is mapped correctly`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isUpdate = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isUpdate).isTrue()
    }

    @Test
    fun `test that timestamp is mapped correctly`() {
        val node = createMockFileNode()
        val timestamp = 1234567890L
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            timestamp = timestamp,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        // Note: formatTime() and formatDate() are @Composable functions and cannot be tested in unit tests
        // We only verify that the timestamp is correctly stored
        assertThat(result.timestampText.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `test that favourite is false for multiple nodes`() {
        val node1 = createMockFileNode(isFavourite = true)
        val node2 = createMockFileNode(isFavourite = true)
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isFavourite).isFalse()
    }

    @Test
    fun `test that nodeLabel is null for multiple nodes`() {
        val node1 = createMockFileNode(nodeLabel = NodeLabel.BLUE)
        val node2 = createMockFileNode(nodeLabel = NodeLabel.GREEN)
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.nodeLabel).isNull()
    }

    @Test
    fun `test that bucket is preserved in result`() {
        val node = createMockFileNode()
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.bucket).isEqualTo(bucket)
    }

    @Test
    fun `test that file type icon mapper is used for non-media buckets`() {
        val node = createMockFileNode(extension = "pdf")
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isMedia = false,
            isNodeKeyDecrypted = true
        )
        val expectedIcon = IconPackR.drawable.ic_pdf_medium_solid
        whenever(fileTypeIconMapper("pdf")).thenReturn(expectedIcon)

        val result = underTest(bucket)

        assertThat(result.icon).isEqualTo(expectedIcon)
    }

    @Test
    fun `test that isSensitive is true for single node when node is marked sensitive`() {
        val node = createMockFileNode(
            name = "sensitive.txt",
            isMarkedSensitive = true,
            isSensitiveInherited = false
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isTrue()
    }

    @Test
    fun `test that isSensitive is true for single node when node has sensitive inherited`() {
        val node = createMockFileNode(
            name = "inherited_sensitive.txt",
            isMarkedSensitive = false,
            isSensitiveInherited = true
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isTrue()
    }

    @Test
    fun `test that isSensitive is true for single node when both marked sensitive and inherited`() {
        val node = createMockFileNode(
            name = "both_sensitive.txt",
            isMarkedSensitive = true,
            isSensitiveInherited = true
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isTrue()
    }

    @Test
    fun `test that isSensitive is false for single node when node is not marked sensitive and not inherited`() {
        val node = createMockFileNode(
            name = "normal.txt",
            isMarkedSensitive = false,
            isSensitiveInherited = false
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isFalse()
    }

    @Test
    fun `test that isSensitive is false for multiple nodes even if one is marked sensitive`() {
        val node1 = createMockFileNode(
            name = "sensitive.txt",
            isMarkedSensitive = true,
            isSensitiveInherited = false
        )
        val node2 = createMockFileNode(
            name = "normal.txt",
            isMarkedSensitive = false,
            isSensitiveInherited = false
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isFalse()
    }

    @Test
    fun `test that isSensitive is false for multiple nodes even if one has sensitive inherited`() {
        val node1 = createMockFileNode(
            name = "inherited_sensitive.txt",
            isMarkedSensitive = false,
            isSensitiveInherited = true
        )
        val node2 = createMockFileNode(
            name = "normal.txt",
            isMarkedSensitive = false,
            isSensitiveInherited = false
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(node1, node2),
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isFalse()
    }

    @Test
    fun `test that isSensitive is false for media buckets even when marked sensitive`() {
        val imageNode1 = createMockFileNode(
            name = "image1.jpg",
            mimeType = "image/jpeg",
            isMarkedSensitive = true,
            isSensitiveInherited = false
        )
        val imageNode2 = createMockFileNode(
            name = "image2.png",
            mimeType = "image/png",
            isMarkedSensitive = true,
            isSensitiveInherited = false
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(imageNode1, imageNode2),
            isMedia = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isFalse()
    }

    @Test
    fun `test that isSensitive is false for media buckets even when has sensitive inherited`() {
        val imageNode1 = createMockFileNode(
            name = "image1.jpg",
            mimeType = "image/jpeg",
            isMarkedSensitive = false,
            isSensitiveInherited = true
        )
        val imageNode2 = createMockFileNode(
            name = "image2.png",
            mimeType = "image/png",
            isMarkedSensitive = false,
            isSensitiveInherited = true
        )
        val bucket = createMockRecentActionBucket(
            nodes = listOf(imageNode1, imageNode2),
            isMedia = true,
            isNodeKeyDecrypted = true
        )

        val result = underTest(bucket)

        assertThat(result.isSensitive).isFalse()
    }

    private fun createMockFileNode(
        name: String = "testFile.txt",
        extension: String = "txt",
        mimeType: String = "text/plain",
        isFavourite: Boolean = false,
        nodeLabel: NodeLabel? = null,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        on { it.isFavourite }.thenReturn(isFavourite)
        on { it.nodeLabel }.thenReturn(nodeLabel)
        on { it.isMarkedSensitive }.thenReturn(isMarkedSensitive)
        on { it.isSensitiveInherited }.thenReturn(isSensitiveInherited)
        val fileTypeInfo = when {
            mimeType.startsWith("image/") -> StaticImageFileTypeInfo(mimeType, extension)
            mimeType.startsWith("video/") -> VideoFileTypeInfo(
                mimeType,
                extension,
                kotlin.time.Duration.ZERO
            )

            else -> TextFileTypeInfo(mimeType, extension)
        }
        on { it.type }.thenReturn(fileTypeInfo)
    }

    private fun createMockRecentActionBucket(
        nodes: List<TypedFileNode>,
        timestamp: Long = 1234567890L,
        isUpdate: Boolean = false,
        isMedia: Boolean = false,
        currentUserIsOwner: Boolean = true,
        userName: String = "TestUser",
        parentFolderName: String = "TestFolder",
        parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
        isNodeKeyDecrypted: Boolean = true,
    ): RecentActionBucket = mock {
        on { it.nodes }.thenReturn(nodes)
        on { it.timestamp }.thenReturn(timestamp)
        on { it.isUpdate }.thenReturn(isUpdate)
        on { it.isMedia }.thenReturn(isMedia)
        on { it.currentUserIsOwner }.thenReturn(currentUserIsOwner)
        on { it.userName }.thenReturn(userName)
        on { it.parentFolderName }.thenReturn(parentFolderName)
        on { it.parentFolderSharesType }.thenReturn(parentFolderSharesType)
        on { it.isNodeKeyDecrypted }.thenReturn(isNodeKeyDecrypted)
    }
}

