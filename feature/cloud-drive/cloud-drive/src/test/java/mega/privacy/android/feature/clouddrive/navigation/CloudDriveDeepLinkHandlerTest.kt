package mega.privacy.android.feature.clouddrive.navigation

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileNodeContentToNavKeyMapper
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeIdFromBase64UseCase
import mega.privacy.android.feature.clouddrive.presentation.shares.links.OpenPasswordLinkDialogNavKey
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveDeepLinkHandlerTest {
    private lateinit var underTest: CloudDriveDeepLinkHandler

    private val getNodeIdFromBase64UseCase = mock<GetNodeIdFromBase64UseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getFileNodeContentForFileNodeUseCase = mock<GetFileNodeContentForFileNodeUseCase>()
    private val fileNodeContentToNavKeyMapper = mock<FileNodeContentToNavKeyMapper>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CloudDriveDeepLinkHandler(
            getNodeIdFromBase64UseCase = getNodeIdFromBase64UseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            fileNodeContentToNavKeyMapper = fileNodeContentToNavKeyMapper,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            getNodeIdFromBase64UseCase,
            getNodeByIdUseCase,
            getFileNodeContentForFileNodeUseCase,
            fileNodeContentToNavKeyMapper,
            getAncestorsIdsUseCase,
        )
    }

    @Test
    fun `test that correct nav key is returned when uri matches PASSWORD_LINK pattern type`() =
        runTest {
            val uriString = "https://mega.nz/encryptedLink"
            val expected = OpenPasswordLinkDialogNavKey(uriString)
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.PASSWORD_LINK)

            assertThat(actual).containsExactly(expected)
        }

    @Test
    fun `test that null is returned when regex pattern type is not HANDLE_LINK or PASSWORD_LINK`() =
        runTest {
            val uriString = "https://other-link"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
            }

            val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK)

            assertThat(actual).isNull()
        }

    @Test
    fun `test that root cloud drive is returned when base64 handle cannot be converted to node id`() =
        runTest {
            val base64Handle = "invalidBase64"
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn null

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(CloudDriveNavKey())
        }

    @Test
    fun `test that root cloud drive is returned when node is not found`() =
        runTest {
            val base64Handle = "validBase64"
            val nodeId = NodeId(123L)
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn null

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(CloudDriveNavKey())
        }

    @Test
    fun `test that correct nav keys are returned for folder node`() =
        runTest {
            val base64Handle = "validBase64"
            val nodeId = NodeId(123L)
            val parentId = NodeId(456L)
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            val folderNode = createMockFolderNode(
                id = nodeId.longValue,
                parentId = parentId,
            )
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn folderNode
            whenever(getAncestorsIdsUseCase(folderNode)) doReturn listOf(parentId)

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(
                CloudDriveNavKey(nodeHandle = parentId.longValue),
                CloudDriveNavKey(nodeHandle = nodeId.longValue),
            ).inOrder()
        }

    @Test
    fun `test that correct nav key is returned for file node without preview`() =
        runTest {
            val base64Handle = "validBase64"
            val nodeId = NodeId(123L)
            val parentId = NodeId(456L)
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            val fileNode = createMockFileNode(
                id = nodeId.longValue,
                parentId = parentId
            )
            val fileNodeContent = FileNodeContent.Other(localFile = null)
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
            whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
            whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
            whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId)

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    highlightedNodeHandle = nodeId.longValue
                )
            )
        }

    @Test
    fun `test that correct nav keys are returned for file node with parent chain`() =
        runTest {
            val base64Handle = "validBase64"
            val nodeId = NodeId(123L)
            val parentId = NodeId(456L)
            val grandParentId = NodeId(789L)
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            val fileNode = createMockFileNode(
                id = nodeId.longValue,
                parentId = parentId,
            )
            val fileNodeContent = FileNodeContent.Other(localFile = null)
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
            whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
            whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
            whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId)

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(
                CloudDriveNavKey(nodeHandle = grandParentId.longValue),
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    highlightedNodeHandle = nodeId.longValue
                )
            ).inOrder()
        }

    @Test
    fun `test that fragment with slash is handled correctly`() =
        runTest {
            val base64Handle = "validBase64"
            val extraPath = "/some/path"
            val nodeId = NodeId(123L)
            val uriString = "https://mega.nz/#$base64Handle$extraPath"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn "$base64Handle$extraPath"
            }
            val folderNode = createMockFolderNode(
                id = nodeId.longValue,
                parentId = NodeId(-1L),
            )
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn folderNode
            whenever(getAncestorsIdsUseCase(folderNode)) doReturn emptyList()

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(
                CloudDriveNavKey(nodeHandle = 123L)
            )
        }

    @Test
    fun `test that preview nav key is returned for file node with preview`() =
        runTest {
            val base64Handle = "validBase64"
            val nodeId = NodeId(123L)
            val parentId = NodeId(456L)
            val uriString = "https://mega.nz/#$base64Handle"
            val uri = mock<Uri> {
                on { this.toString() } doReturn uriString
                on { this.fragment } doReturn base64Handle
            }
            val fileNode = createMockFileNode(
                id = nodeId.longValue,
                parentId = parentId
            )
            val fileNodeContent = FileNodeContent.Pdf(
                uri = mega.privacy.android.domain.entity.node.NodeContentUri.RemoteContentUri(
                    "http://example.com/file.pdf",
                    false
                )
            )
            val previewNavKey = LegacyPdfViewerNavKey(
                nodeHandle = nodeId.longValue,
                nodeContentUri = fileNodeContent.uri,
                nodeSourceType = null,
                mimeType = "application/pdf"
            )
            whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
            whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
            whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
            whenever(
                fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)
            ) doReturn previewNavKey
            whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId)

            val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK)

            assertThat(actual).containsExactly(
                CloudDriveNavKey(nodeHandle = parentId.longValue),
                previewNavKey
            ).inOrder()
        }

    private fun createMockFileNode(
        id: Long,
        parentId: NodeId = NodeId(-1),
    ): TypedFileNode = mock<DefaultTypedFileNode> {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.parentId).thenReturn(parentId)
    }

    private fun createMockFolderNode(
        id: Long,
        parentId: NodeId = NodeId(-1),
    ): TypedFolderNode = mock<DefaultTypedFolderNode> {
        whenever(it.id).thenReturn(NodeId(id))
        whenever(it.parentId).thenReturn(parentId)
    }
}

