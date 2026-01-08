package mega.privacy.android.core.nodecomponents.mapper


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.model.NodeSubtitleText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class NodeUiItemMapperTest {

    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val nodeSubtitleMapper: NodeSubtitleMapper = mock()

    private lateinit var underTest: NodeUiItemMapper

    @BeforeEach
    fun setUp() {
        whenever(durationInSecondsTextMapper(any())).thenReturn("1:30")
        whenever(
            nodeSubtitleMapper(
                any(),
                any()
            )
        ).thenReturn(
            NodeSubtitleText.FileSubtitle(
                fileSizeValue = 1,
                modificationTime = 1234567890L,
                showPublicLinkCreationTime = false
            )
        )
        setupDefaultFileTypeIconMocks()
        underTest = NodeUiItemMapper(
            fileTypeIconMapper = fileTypeIconMapper,
            durationInSecondsTextMapper = durationInSecondsTextMapper,
            nodeSubtitleMapper = nodeSubtitleMapper,
            ioDispatcher = StandardTestDispatcher()
        )
    }

    private fun setupDefaultFileTypeIconMocks() {
        whenever(fileTypeIconMapper("")).thenReturn(IconPackR.drawable.ic_folder_medium_solid)
        whenever(fileTypeIconMapper("txt")).thenReturn(IconPackR.drawable.ic_text_medium_solid)
    }

    private fun createMockFolderNode(
        id: Long = 1L,
        name: String = "Test Folder",
        isFavourite: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        isNodeKeyDecrypted: Boolean = true,
        description: String? = null,
        tags: List<String> = emptyList(),
        childFolderCount: Int = 2,
        childFileCount: Int = 3,
    ): TypedFolderNode = mock {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(0L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(0)
        whenever(it.isFavourite).thenReturn(isFavourite)
        whenever(it.isMarkedSensitive).thenReturn(isMarkedSensitive)
        whenever(it.isSensitiveInherited).thenReturn(isSensitiveInherited)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(description)
        whenever(it.tags).thenReturn(tags)
        whenever(it.childFolderCount).thenReturn(childFolderCount)
        whenever(it.childFileCount).thenReturn(childFileCount)
        whenever(it.type).thenReturn(FolderType.Default)
    }

    private fun createMockFileNode(
        id: Long = 2L,
        name: String = "test_file.txt",
        isFavourite: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
        isIncomingShare: Boolean = false,
        description: String? = null,
        tags: List<String> = emptyList(),
        size: Long = 1024L,
        label: Int = 0,
    ): TypedFileNode = mock {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(1L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(label)
        whenever(it.isFavourite).thenReturn(isFavourite)
        whenever(it.isMarkedSensitive).thenReturn(isMarkedSensitive)
        whenever(it.isSensitiveInherited).thenReturn(isSensitiveInherited)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(true)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(description)
        whenever(it.tags).thenReturn(tags)
        whenever(it.size).thenReturn(size)
        whenever(it.modificationTime).thenReturn(1234567890L)
        whenever(it.hasThumbnail).thenReturn(false)
        whenever(it.hasPreview).thenReturn(false)
        whenever(it.type).thenReturn(TextFileTypeInfo("text/plain", "txt"))
    }

    private fun createMockShareFolderNode(
        id: Long = 3L,
        name: String = "Shared Folder",
        isIncomingShare: Boolean = true,
        isNodeKeyDecrypted: Boolean = false,
        shareData: ShareData? = null,
    ): ShareFolderNode = mock {
        whenever(it.node).thenReturn(mock<TypedFolderNode>())
        whenever(it.shareData).thenReturn(shareData)
        // Set up all TypedFolderNode properties directly
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(0L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(0)
        whenever(it.isFavourite).thenReturn(false)
        whenever(it.isMarkedSensitive).thenReturn(false)
        whenever(it.isSensitiveInherited).thenReturn(false)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(null)
        whenever(it.tags).thenReturn(emptyList())
        whenever(it.childFolderCount).thenReturn(2)
        whenever(it.childFileCount).thenReturn(3)
        whenever(it.type).thenReturn(FolderType.Default)
    }

    private fun createMockShareFileNode(
        id: Long = 3L,
        name: String = "Shared Folder",
        isIncomingShare: Boolean = true,
        isNodeKeyDecrypted: Boolean = false,
        shareData: ShareData? = null,
    ): ShareFileNode = mock {
        whenever(it.node).thenReturn(mock<TypedFileNode>())
        whenever(it.shareData).thenReturn(shareData)
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.name).thenReturn(name)
        whenever(it.parentId).thenReturn(NodeId(0L))
        whenever(it.base64Id).thenReturn("test_base64_id")
        whenever(it.label).thenReturn(0)
        whenever(it.isFavourite).thenReturn(false)
        whenever(it.isMarkedSensitive).thenReturn(false)
        whenever(it.isSensitiveInherited).thenReturn(false)
        whenever(it.isTakenDown).thenReturn(false)
        whenever(it.isIncomingShare).thenReturn(isIncomingShare)
        whenever(it.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
        whenever(it.creationTime).thenReturn(1234567890L)
        whenever(it.isAvailableOffline).thenReturn(false)
        whenever(it.versionCount).thenReturn(0)
        whenever(it.description).thenReturn(null)
        whenever(it.tags).thenReturn(emptyList())
        whenever(it.type).thenReturn(TextFileTypeInfo("text/plain", "txt"))
    }

    private fun createMockShareData(
        user: String? = "test@example.com",
        userFullName: String? = "Test User",
        nodeHandle: Long = 123L,
        access: AccessPermission = AccessPermission.READ,
        timeStamp: Long = 1234567890L,
        isPending: Boolean = false,
        isVerified: Boolean = false,
        isContactCredentialsVerified: Boolean = false,
        count: Int = 0,
    ): ShareData = ShareData(
        user = user,
        userFullName = userFullName,
        nodeHandle = nodeHandle,
        access = access,
        timeStamp = timeStamp,
        isPending = isPending,
        isVerified = isVerified,
        isContactCredentialsVerified = isContactCredentialsVerified,
        count = count
    )

    @Test
    fun `test that invoke returns correct NodeUiItem when mapping folder node`() = runTest {
        val mockFolderNode = createMockFolderNode()

        val result = underTest(
            nodeList = listOf(mockFolderNode),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        val nodeUiItem = result[0]
        assertThat(nodeUiItem.node).isEqualTo(mockFolderNode)
        assertThat(nodeUiItem.isFolderNode).isTrue()
        assertThat(nodeUiItem.isVideoNode).isFalse()
        assertThat(nodeUiItem.iconRes).isEqualTo(IconPackR.drawable.ic_folder_medium_solid)
        assertThat(nodeUiItem.title).isEqualTo(LocalizedText.Literal("Test Folder"))
        assertThat(nodeUiItem.isSelected).isFalse()
        assertThat(nodeUiItem.isHighlighted).isFalse()
        assertThat(nodeUiItem.showLink).isFalse()
        assertThat(nodeUiItem.showFavourite).isFalse()
        assertThat(nodeUiItem.isSensitive).isFalse()
        assertThat(nodeUiItem.showBlurEffect).isFalse()
        assertThat(nodeUiItem.duration).isNull()
    }

    @Test
    fun `test that invoke returns correct NodeUiItem when mapping file node`() = runTest {
        val mockFileNode = createMockFileNode()

        val result = underTest(
            nodeList = listOf(mockFileNode),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        val nodeUiItem = result[0]
        assertThat(nodeUiItem.node).isEqualTo(mockFileNode)
        assertThat(nodeUiItem.isFolderNode).isFalse()
        assertThat(nodeUiItem.isVideoNode).isFalse()
        assertThat(nodeUiItem.iconRes).isEqualTo(IconPackR.drawable.ic_text_medium_solid)
        assertThat(nodeUiItem.title).isEqualTo(LocalizedText.Literal("test_file.txt"))
        assertThat(nodeUiItem.isSelected).isFalse()
        assertThat(nodeUiItem.isHighlighted).isFalse()
        assertThat(nodeUiItem.showLink).isFalse()
        assertThat(nodeUiItem.showFavourite).isFalse()
        assertThat(nodeUiItem.isSensitive).isFalse()
        assertThat(nodeUiItem.showBlurEffect).isFalse()
        assertThat(nodeUiItem.duration).isNull()
    }

    @Test
    fun `test that invoke sets isHighlighted to true when highlightedNodeId matches node id`() =
        runTest {
            val mockFolderNode = createMockFolderNode()

            val result = underTest(
                nodeList = listOf(mockFolderNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                highlightedNodeId = NodeId(1L),
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isHighlighted).isTrue()
        }

    @Test
    fun `test that invoke sets isHighlighted to true when highlightedNames contains node name`() =
        runTest {
            val mockFolderNode = createMockFolderNode()

            val result = underTest(
                nodeList = listOf(mockFolderNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                highlightedNames = listOf("Test Folder"),
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isHighlighted).isTrue()
        }

    @Test
    fun `test that invoke sets showBlurEffect to true when node is sensitive and has supported file type`() =
        runTest {
            val mockSensitiveImageNode = createMockFileNode(
                id = 4L,
                name = "sensitive_image.jpg",
                isMarkedSensitive = true,
            )
            whenever(mockSensitiveImageNode.type).thenReturn(
                StaticImageFileTypeInfo("image/jpeg", "jpg")
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveImageNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isTrue()
            assertThat(result[0].showBlurEffect).isTrue()
        }

    @Test
    fun `test that invoke sets showBlurEffect to false when node is not sensitive and has supported file type`() =
        runTest {
            val mockSensitiveImageNode = createMockFileNode(
                id = 4L,
                name = "sensitive_image.jpg",
                isMarkedSensitive = false,
            )
            whenever(mockSensitiveImageNode.type).thenReturn(
                StaticImageFileTypeInfo("image/jpeg", "jpg")
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveImageNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isFalse()
            assertThat(result[0].showBlurEffect).isFalse()
        }

    @Test
    fun `test that invoke sets isSensitive to false when nodeSourceType is INCOMING_SHARES`() =
        runTest {
            val mockSensitiveNode = createMockFileNode(
                id = 4L,
                name = "sensitive_file.txt",
                isMarkedSensitive = true,
            )

            val result = underTest(
                nodeList = listOf(mockSensitiveNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].isSensitive).isFalse()
        }

    @Test
    fun `test that invoke sets showLink to true when node has exportedData`() = runTest {
        val mockExportedData = mock<mega.privacy.android.domain.entity.node.ExportedData> {
            whenever(it.publicLink).thenReturn("https://mega.app/test")
            whenever(it.publicLinkCreationTime).thenReturn(1234567890L)
        }
        val mockNodeWithLink = createMockFileNode(
            id = 5L,
            name = "shared_file.txt",
        )
        whenever(mockNodeWithLink.exportedData).thenReturn(mockExportedData)

        val result = underTest(
            nodeList = listOf(mockNodeWithLink),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].showLink).isTrue()
    }

    @Test
    fun `test that invoke sets showFavourite to true when node is favourite and not incoming share`() =
        runTest {
            val mockFavouriteNode = createMockFileNode(
                id = 6L,
                name = "favourite_file.txt",
                isFavourite = true,
            )

            val result = underTest(
                nodeList = listOf(mockFavouriteNode),
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].showFavourite).isTrue()
        }

    @Test
    fun `test that invoke sets showFavourite to false when node is favourite but is incoming share`() =
        runTest {
            val mockFavouriteIncomingShare = createMockFileNode(
                id = 7L,
                name = "favourite_incoming_share.txt",
                isFavourite = true,
                isIncomingShare = true,
            )

            val result = underTest(
                nodeList = listOf(mockFavouriteIncomingShare),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].showFavourite).isFalse()
        }

    @Test
    fun `test that invoke sets formattedDescription when node has description`() = runTest {
        val mockNodeWithDescription = createMockFileNode(
            id = 9L,
            name = "file_with_description.txt",
            description = "This is a test description",
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithDescription),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].formattedDescription).isEqualTo(
            LocalizedText.Literal("This is a test description")
        )
    }

    @Test
    fun `test that invoke replaces newlines with spaces in formattedDescription`() = runTest {
        val mockNodeWithDescription = createMockFileNode(
            id = 10L,
            name = "file_with_description_newlines.txt",
            description = "This is a test description\nwith newlines",
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithDescription),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].formattedDescription).isEqualTo(
            LocalizedText.Literal("This is a test description with newlines")
        )
    }

    @Test
    fun `test that invoke sets tags when nodeSourceType is not RUBBISH_BIN`() = runTest {
        val mockNodeWithTags = createMockFileNode(
            id = 11L,
            name = "file_with_tags.txt",
            tags = listOf("tag1", "tag2"),
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithTags),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].tags).isEqualTo(listOf("tag1", "tag2"))
    }

    @Test
    fun `test that invoke sets tags to null when nodeSourceType is RUBBISH_BIN`() = runTest {
        val mockNodeWithTags = createMockFileNode(
            id = 11L,
            name = "file_with_tags.txt",
            tags = listOf("tag1", "tag2"),
        )

        val result = underTest(
            nodeList = listOf(mockNodeWithTags),
            nodeSourceType = NodeSourceType.RUBBISH_BIN,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].tags).isNull()
    }

    @Test
    fun `test that invoke maps multiple nodes correctly`() = runTest {
        val mockFolderNode = createMockFolderNode()
        val mockFileNode = createMockFileNode()
        val nodeList = listOf(mockFolderNode, mockFileNode)

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(2)
        assertThat(result[0].isFolderNode).isTrue()
        assertThat(result[1].isFolderNode).isFalse()
    }

    @Test
    fun `test that invoke returns StringRes title for incoming share folder with undecrypted node key`() =
        runTest {
            val mockShareData = createMockShareData(
            )
            val mockShareFolderNode = createMockShareFolderNode(
                name = "Undecrypted Folder",
                isIncomingShare = true,
                isNodeKeyDecrypted = false,
                shareData = mockShareData
            )

            val result = underTest(
                nodeList = listOf(mockShareFolderNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(
                LocalizedText.StringRes(R.string.shared_items_verify_credentials_undecrypted_folder)
            )
        }

    @Test
    fun `test that invoke returns Literal title for incoming share folder with decrypted node key`() =
        runTest {
            val mockShareData = createMockShareData(
            )
            val mockShareFolderNode = createMockShareFolderNode(
                name = "Decrypted Folder",
                isIncomingShare = true,
                isNodeKeyDecrypted = true,
                shareData = mockShareData
            )

            val result = underTest(
                nodeList = listOf(mockShareFolderNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(LocalizedText.Literal("Decrypted Folder"))
        }

    @Test
    fun `test that invoke returns PluralsRes title for incoming share file with undecrypted node key`() =
        runTest {
            val mockShareData = createMockShareData(
            )
            val mockShareFileNode = createMockShareFileNode(
                name = "Undecrypted File",
                isIncomingShare = true,
                isNodeKeyDecrypted = false,
                shareData = mockShareData
            )

            val result = underTest(
                nodeList = listOf(mockShareFileNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(
                LocalizedText.PluralsRes(
                    R.plurals.shared_items_verify_credentials_undecrypted_file,
                    1
                )
            )
        }

    @Test
    fun `test that invoke returns Literal title for incoming share file with decrypted node key`() =
        runTest {
            val mockShareData = createMockShareData(
            )
            val mockShareFileNode = createMockShareFileNode(
                name = "Decrypted File",
                isIncomingShare = true,
                isNodeKeyDecrypted = true,
                shareData = mockShareData
            )

            val result = underTest(
                nodeList = listOf(mockShareFileNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(LocalizedText.Literal("Decrypted File"))
        }

    @Test
    fun `test that invoke returns Literal title for regular folder node`() = runTest {
        val mockFolderNode = createMockFolderNode(
            name = "Regular Folder",
            isIncomingShare = false
        )

        val result = underTest(
            nodeList = listOf(mockFolderNode),
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo(LocalizedText.Literal("Regular Folder"))
    }

    @Test
    fun `test that invoke returns StringRes title for share folder node without share data when node key is not decrypted`() =
        runTest {
            val mockShareFolderNode = createMockShareFolderNode(
                name = "Share Folder Without Data",
                isIncomingShare = true,
                isNodeKeyDecrypted = false,
                shareData = null // No share data
            )

            val result = underTest(
                nodeList = listOf(mockShareFolderNode),
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].title).isEqualTo(
                LocalizedText.StringRes(R.string.shared_items_verify_credentials_undecrypted_folder)
            )
        }

    // Helper function to create NodeUiItem<TypedNode> for testing
    private fun createNodeUiItem(
        node: TypedNode,
        isSelected: Boolean = false,
        isHighlighted: Boolean = false,
    ): NodeUiItem<TypedNode> = NodeUiItem(
        node = node,
        isSelected = isSelected,
        isHighlighted = isHighlighted,
        title = LocalizedText.Literal(node.name),
        subtitle = NodeSubtitleText.FileSubtitle(
            fileSizeValue = 1,
            modificationTime = 1234567890L,
            showPublicLinkCreationTime = false
        ),
        formattedDescription = null,
        tags = emptyList(),
        iconRes = if (node is TypedFolderNode) IconPackR.drawable.ic_folder_medium_solid
        else IconPackR.drawable.ic_text_medium_solid,
        thumbnailData = null,
        accessPermissionIcon = null,
        showIsVerified = false,
        showLink = false,
        showFavourite = false,
        isSensitive = false,
        showBlurEffect = false,
        isFolderNode = node is TypedFolderNode,
        isVideoNode = false,
        duration = null,
    )

    @Test
    fun `test that invoke preserves selection state when existingItems is provided`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode = createMockFileNode(id = 2L, name = "File 1")
        val nodeList = listOf(folderNode, fileNode)

        // Create existing items with some selected
        val existingItems = listOf(
            createNodeUiItem(node = folderNode, isSelected = true),
            createNodeUiItem(node = fileNode, isSelected = false)
        )

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = existingItems,
        )

        assertThat(result).hasSize(2)
        // Folder should remain selected
        assertThat(result[0].isSelected).isTrue()
        assertThat(result[0].node.id).isEqualTo(NodeId(1L))
        // File should remain unselected
        assertThat(result[1].isSelected).isFalse()
        assertThat(result[1].node.id).isEqualTo(NodeId(2L))
    }

    @Test
    fun `test that invoke defaults to false when existingItems is null`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode = createMockFileNode(id = 2L, name = "File 1")
        val nodeList = listOf(folderNode, fileNode)

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = null, // No existing items provided
        )

        assertThat(result).hasSize(2)
        // Both should default to unselected
        assertThat(result[0].isSelected).isFalse()
        assertThat(result[1].isSelected).isFalse()
    }

    @Test
    fun `test that invoke defaults to false when existingItems is empty`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode = createMockFileNode(id = 2L, name = "File 1")
        val nodeList = listOf(folderNode, fileNode)

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = emptyList(), // Empty existing items
        )

        assertThat(result).hasSize(2)
        // Both should default to unselected
        assertThat(result[0].isSelected).isFalse()
        assertThat(result[1].isSelected).isFalse()
    }

    @Test
    fun `test that invoke handles partial selection state preservation`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode1 = createMockFileNode(id = 2L, name = "File 1")
        val fileNode2 = createMockFileNode(id = 3L, name = "File 2")
        val nodeList = listOf(folderNode, fileNode1, fileNode2)

        // Create existing items with only some nodes (simulating new nodes added)
        val existingItems = listOf(
            createNodeUiItem(node = folderNode, isSelected = true),
            createNodeUiItem(node = fileNode1, isSelected = false)
            // Note: fileNode2 (id=3) is not in existing items (new node)
        )

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = existingItems,
        )

        assertThat(result).hasSize(3)
        // Folder should remain selected (from existing items)
        assertThat(result[0].isSelected).isTrue()
        assertThat(result[0].node.id).isEqualTo(NodeId(1L))
        // File 1 should remain unselected (from existing items)
        assertThat(result[1].isSelected).isFalse()
        assertThat(result[1].node.id).isEqualTo(NodeId(2L))
        // File 2 should default to unselected (new node, not in existing items)
        assertThat(result[2].isSelected).isFalse()
        assertThat(result[2].node.id).isEqualTo(NodeId(3L))
    }

    @Test
    fun `test that invoke handles all nodes selected in existing items`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode = createMockFileNode(id = 2L, name = "File 1")
        val nodeList = listOf(folderNode, fileNode)

        // Create existing items with all selected
        val existingItems = listOf(
            createNodeUiItem(node = folderNode, isSelected = true),
            createNodeUiItem(node = fileNode, isSelected = true)
        )

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = existingItems,
        )

        assertThat(result).hasSize(2)
        // Both should remain selected
        assertThat(result[0].isSelected).isTrue()
        assertThat(result[1].isSelected).isTrue()
    }

    @Test
    fun `test that invoke preserves selection state with different node order`() = runTest {
        val folderNode = createMockFolderNode(id = 1L, name = "Folder 1")
        val fileNode = createMockFileNode(id = 2L, name = "File 1")

        // Original order: folder, file
        val nodeList = listOf(folderNode, fileNode)

        // Existing items in different order: file, folder
        val existingItems = listOf(
            createNodeUiItem(node = fileNode, isSelected = true),
            createNodeUiItem(node = folderNode, isSelected = false)
        )

        val result = underTest(
            nodeList = nodeList,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            existingItems = existingItems,
        )

        assertThat(result).hasSize(2)
        // Result order should match nodeList order, but selection should be preserved by ID
        // First result is folder (id=1) - should be unselected
        assertThat(result[0].node.id).isEqualTo(NodeId(1L))
        assertThat(result[0].isSelected).isFalse()
        // Second result is file (id=2) - should be selected
        assertThat(result[1].node.id).isEqualTo(NodeId(2L))
        assertThat(result[1].isSelected).isTrue()
    }
} 