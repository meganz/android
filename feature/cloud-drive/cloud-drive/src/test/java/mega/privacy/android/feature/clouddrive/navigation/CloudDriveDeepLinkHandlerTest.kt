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
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import mega.privacy.android.domain.usecase.node.GetFileNodeContentForFileNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeIdFromBase64UseCase
import mega.privacy.android.domain.usecase.node.IsNodeInCloudDriveUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishBinUseCase
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudDriveDeepLinkHandlerTest {
    private lateinit var underTest: CloudDriveDeepLinkHandler

    private val snackbarEventQueue = mock<SnackbarEventQueue>()

    private val getNodeIdFromBase64UseCase = mock<GetNodeIdFromBase64UseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getFileNodeContentForFileNodeUseCase = mock<GetFileNodeContentForFileNodeUseCase>()
    private val fileNodeContentToNavKeyMapper = mock<FileNodeContentToNavKeyMapper>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()
    private val isNodeInCloudDriveUseCase = mock<IsNodeInCloudDriveUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()


    @BeforeAll
    fun setup() {
        underTest = CloudDriveDeepLinkHandler(
            getNodeIdFromBase64UseCase = getNodeIdFromBase64UseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getFileNodeContentForFileNodeUseCase = getFileNodeContentForFileNodeUseCase,
            fileNodeContentToNavKeyMapper = fileNodeContentToNavKeyMapper,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
            snackbarEventQueue = snackbarEventQueue,
            isNodeInCloudDriveUseCase = isNodeInCloudDriveUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
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
            snackbarEventQueue,
            isNodeInCloudDriveUseCase,
            isNodeInRubbishBinUseCase,
        )
        wheneverBlocking { isNodeInCloudDriveUseCase(any()) } doReturn true
        wheneverBlocking { isNodeInRubbishBinUseCase(anyValueClass()) } doReturn false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that null is returned when regex pattern type is not HANDLE_LINK`(
        isLoggedIn: Boolean,
    ) = runTest {
        val uriString = "https://other-link"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
        }

        val actual = underTest.getNavKeys(uri, RegexPatternType.FILE_LINK, isLoggedIn)

        assertThat(actual).isNull()
        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that empty list is returned when uri matches HANDLE_LINK but an exception is thrown`(
        isLoggedIn: Boolean,
    ) = runTest {
        val base64Handle = "invalidBase64"
        val uriString = "https://mega.nz/#$base64Handle"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
            on { this.fragment } doReturn base64Handle
        }
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doThrow (RuntimeException())

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        assertThat(actual).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that root cloud drive is returned when base64 handle cannot be converted to node id`(
        isLoggedIn: Boolean,
    ) = runTest {
        val base64Handle = "invalidBase64"
        val uriString = "https://mega.nz/#$base64Handle"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
            on { this.fragment } doReturn base64Handle
        }
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn null

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey()
                )
            )
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that root cloud drive is returned when node is not found`(
        isLoggedIn: Boolean,
    ) = runTest {
        val base64Handle = "validBase64"
        val nodeId = NodeId(123L)
        val uriString = "https://mega.nz/#$base64Handle"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
            on { this.fragment } doReturn base64Handle
        }
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn null

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey()
                )
            )
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for folder node`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        whenever(getAncestorsIdsUseCase(folderNode)) doReturn listOf(parentId, rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey(),
                    listOf(
                        CloudDriveNavKey(nodeHandle = parentId.longValue),
                        CloudDriveNavKey(nodeHandle = nodeId.longValue),
                    ),
                )
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav key is returned for file node without preview`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey(),
                    listOf(
                        CloudDriveNavKey(
                            nodeHandle = parentId.longValue,
                            highlightedNodeHandle = nodeId.longValue
                        )
                    )
                )
            )
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for file node with parent chain`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId, rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey(),
                    listOf(
                        CloudDriveNavKey(nodeHandle = grandParentId.longValue),
                        CloudDriveNavKey(
                            nodeHandle = parentId.longValue,
                            highlightedNodeHandle = nodeId.longValue
                        ),
                    )
                )
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that fragment with slash is handled correctly`(
        isLoggedIn: Boolean,
    ) = runTest {
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

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey(),
                    listOf(CloudDriveNavKey(nodeHandle = 123L)),
                )
            )
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that preview nav key is returned for file node with preview`(
        isLoggedIn: Boolean,
    ) = runTest {
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

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                HomeScreensNavKey(
                    DriveSyncNavKey(),
                ),
                previewNavKey
            )

            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @Test
    fun `test that node is highlighted if it's in root cloud drive and has no preview`() = runTest {
        val base64Handle = "validBase64"
        val nodeId = NodeId(123L)
        val uriString = "https://mega.nz/#$base64Handle"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
            on { this.fragment } doReturn base64Handle
        }
        val fileNode = createMockFileNode(
            id = nodeId.longValue,
        )
        val fileNodeContent = FileNodeContent.Other(localFile = null)
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn true
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, true)

        assertThat(actual).containsExactly(
            HomeScreensNavKey(DriveSyncNavKey(highlightedNodeHandle = nodeId.longValue))
        )
        verifyNoInteractions(snackbarEventQueue)
    }

    @Test
    fun `test that node is highlighted if it's in root rubbish bin and has no preview`() = runTest {
        val base64Handle = "validBase64"
        val nodeId = NodeId(123L)
        val uriString = "https://mega.nz/#$base64Handle"
        val uri = mock<Uri> {
            on { this.toString() } doReturn uriString
            on { this.fragment } doReturn base64Handle
        }
        val fileNode = createMockFileNode(
            id = nodeId.longValue,
        )
        val fileNodeContent = FileNodeContent.Other(localFile = null)
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
        whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn true
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, true)

        assertThat(actual).containsExactly(
            RubbishBinNavKey(highlightedNodeHandle = nodeId.longValue),
        )
        verifyNoInteractions(snackbarEventQueue)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for file node without preview with parent chain in rubbish bin`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
        whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn true
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId, rootId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                RubbishBinNavKey(),
                CloudDriveNavKey(
                    nodeHandle = grandParentId.longValue,
                    nodeSourceType = NodeSourceType.RUBBISH_BIN
                ),
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    highlightedNodeHandle = nodeId.longValue,
                    nodeSourceType = NodeSourceType.RUBBISH_BIN
                ),
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for file node with preview and parent chain in rubbish bin`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        val fileNodeContent = FileNodeContent.Pdf(
            uri = mega.privacy.android.domain.entity.node.NodeContentUri.RemoteContentUri(
                "http://example.com/file.pdf",
                false
            )
        )
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
        whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn true
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId, rootId)
        val previewNavKey = LegacyPdfViewerNavKey(
            nodeHandle = nodeId.longValue,
            nodeContentUri = fileNodeContent.uri,
            nodeSourceType = null,
            mimeType = "application/pdf"
        )
        whenever(
            fileNodeContentToNavKeyMapper(fileNodeContent, fileNode, NodeSourceType.RUBBISH_BIN)
        ) doReturn previewNavKey

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                RubbishBinNavKey(),
                CloudDriveNavKey(
                    nodeHandle = grandParentId.longValue,
                    nodeSourceType = NodeSourceType.RUBBISH_BIN
                ),
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    nodeSourceType = NodeSourceType.RUBBISH_BIN
                ),
                previewNavKey
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for file node without preview with parent chain in incoming shares`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
        whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn false
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(fileNodeContentToNavKeyMapper(fileNodeContent, fileNode)) doReturn null
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId)

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                mega.privacy.android.navigation.destination.SharesNavKey,
                CloudDriveNavKey(
                    nodeHandle = grandParentId.longValue,
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ),
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    highlightedNodeHandle = nodeId.longValue,
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ),
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that correct nav keys are returned for file node with preview and parent chain in incoming shares`(
        isLoggedIn: Boolean,
    ) = runTest {
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
        val fileNodeContent = FileNodeContent.Pdf(
            uri = mega.privacy.android.domain.entity.node.NodeContentUri.RemoteContentUri(
                "http://example.com/file.pdf",
                false
            )
        )
        whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
        whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn false
        whenever(getNodeIdFromBase64UseCase(base64Handle)) doReturn nodeId
        whenever(getNodeByIdUseCase(nodeId)) doReturn fileNode
        whenever(getFileNodeContentForFileNodeUseCase(fileNode)) doReturn fileNodeContent
        whenever(getAncestorsIdsUseCase(fileNode)) doReturn listOf(parentId, grandParentId)
        val previewNavKey = LegacyPdfViewerNavKey(
            nodeHandle = nodeId.longValue,
            nodeContentUri = fileNodeContent.uri,
            nodeSourceType = null,
            mimeType = "application/pdf"
        )
        whenever(
            fileNodeContentToNavKeyMapper(fileNodeContent, fileNode, NodeSourceType.INCOMING_SHARES)
        ) doReturn previewNavKey

        val actual = underTest.getNavKeys(uri, RegexPatternType.HANDLE_LINK, isLoggedIn)

        if (isLoggedIn) {
            assertThat(actual).containsExactly(
                mega.privacy.android.navigation.destination.SharesNavKey,
                CloudDriveNavKey(
                    nodeHandle = grandParentId.longValue,
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ),
                CloudDriveNavKey(
                    nodeHandle = parentId.longValue,
                    nodeSourceType = NodeSourceType.INCOMING_SHARES,
                ),
                previewNavKey
            ).inOrder()
            verifyNoInteractions(snackbarEventQueue)
        } else {
            assertThat(actual).isEmpty()
            verify(snackbarEventQueue).queueMessage(sharedR.string.general_alert_not_logged_in)
        }
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

    private val rootId = NodeId(1L)
}

