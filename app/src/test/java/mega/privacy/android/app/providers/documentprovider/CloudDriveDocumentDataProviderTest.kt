package mega.privacy.android.app.providers.documentprovider

import android.provider.DocumentsContract.Document
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.GetOpenableLocalFileForCloudDriveSafUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.io.FileNotFoundException

/**
 * Unit tests for [CloudDriveDocumentDataProvider].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class CloudDriveDocumentDataProviderTest {

    private lateinit var testScheduler: TestCoroutineScheduler
    private lateinit var testScope: TestScope
    private val getRootNodeIdUseCase: GetRootNodeIdUseCase = mock()
    private val getNodesByIdInChunkUseCase: GetNodesByIdInChunkUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase = mock()
    private val getAccountCredentialsUseCase: GetAccountCredentialsUseCase = mock()
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val cloudDriveDocumentRowMapper: CloudDriveDocumentRowMapper = mock()
    private val addNodeType: AddNodeType = mock()
    private val documentIdToNodeIdMapper: DocumentIdToNodeIdMapper = mock()
    private val monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase = mock()
    private val getOpenableLocalFileForCloudDriveSafUseCase: GetOpenableLocalFileForCloudDriveSafUseCase =
        mock()
    private val mockedCredentials: UserCredentials = mock()

    private lateinit var underTest: CloudDriveDocumentDataProvider

    private companion object {
        private const val DOCUMENT_ID_PREFIX = "mega_cloud_drive_root"
        private val ROOT_NODE_ID = NodeId(1L)
        private const val ROOT_DOCUMENT_ID = "mega_cloud_drive_root:1"
    }

    @BeforeEach
    fun setUp() {
        testScheduler = TestCoroutineScheduler()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        reset(
            getRootNodeIdUseCase,
            getNodesByIdInChunkUseCase,
            getNodeByHandleUseCase,
            backgroundFastLoginUseCase,
            getAccountCredentialsUseCase,
            monitorNodeUpdatesUseCase,
            monitorUserCredentialsUseCase,
            monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            cloudDriveDocumentRowMapper,
            addNodeType,
            documentIdToNodeIdMapper,
            monitorPasscodeLockPreferenceUseCase,
            getOpenableLocalFileForCloudDriveSafUseCase,
            mockedCredentials,
        )
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(monitorPasscodeLockPreferenceUseCase()).thenReturn(flowOf(false))
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
        whenever(mockedCredentials.email).thenReturn("test@mega.co.nz")
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(mockedCredentials))
        whenever(documentIdToNodeIdMapper.invoke(any(), any())).thenAnswer { invocation ->
            val docId = invocation.getArgument<String>(0)
            val prefix = invocation.getArgument<String>(1)
            if (!docId.startsWith("$prefix:")) null
            else docId.substring(prefix.length + 1).toLongOrNull()?.let { NodeId(it) }
        }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = CloudDriveDocumentDataProvider(
            applicationScope = testScope,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            cloudDriveDocumentRowMapper = cloudDriveDocumentRowMapper,
            addNodeType = addNodeType,
            documentIdToNodeIdMapper = documentIdToNodeIdMapper,
            monitorPasscodeLockPreferenceUseCase = monitorPasscodeLockPreferenceUseCase,
            getOpenableLocalFileForCloudDriveSafUseCase = getOpenableLocalFileForCloudDriveSafUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that state is NotLoggedIn when credentials null`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(null))

        underTest.state.test {
            skipItems(1) // skip Initialising (StateFlow initial value)
            assertThat(awaitItem()).isInstanceOf(CloudDriveDocumentProviderUiState.NotLoggedIn::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is PasscodeLockEnabled when credentials exist and passcode enabled`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(monitorPasscodeLockPreferenceUseCase()).thenReturn(flowOf(true))

            underTest.state.test {
                skipItems(1) // skip Initialising
                val state = awaitItem()
                assertThat(state).isInstanceOf(
                    CloudDriveDocumentProviderUiState.PasscodeLockEnabled::class.java
                )
                assertThat((state as CloudDriveDocumentProviderUiState.PasscodeLockEnabled).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is Offline when credentials exist and updateConnectivity false`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)

            underTest.state.test {
                skipItems(1) // skip Initialising
                awaitItem() // may get RootNodeNotLoaded or other; ensure we're past initial
                underTest.updateConnectivity(false)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.Offline::class.java)
                assertThat((state as CloudDriveDocumentProviderUiState.Offline).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is RootNodeNotLoaded when credentials exist but root node is null`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(null)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)

            underTest.state.test {
                skipItems(1) // skip Initialising
                val state = awaitItem()
                assertThat(state).isInstanceOf(
                    CloudDriveDocumentProviderUiState.RootNodeNotLoaded::class.java
                )
                assertThat((state as CloudDriveDocumentProviderUiState.RootNodeNotLoaded).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that state is DocumentData for root when credentials and root available`() = runTest {
        val mockNode: FolderNode = mock()
        val typedNode: DefaultTypedFolderNode = mock()
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
        val rootRow = CloudDriveDocumentRow(
            documentId = CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
            displayName = "MEGA",
            mimeType = Document.MIME_TYPE_DIR,
            size = 0L,
            lastModified = 0L,
            flags = 0,
        )
        whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(mockNode)
        whenever(addNodeType.invoke(any())).thenReturn(typedNode)
        whenever(cloudDriveDocumentRowMapper.invoke(any(), any())).thenReturn(rootRow)

        underTest.state.test {
            skipItems(1) // skip Initialising (StateFlow initial value)
            awaitItem() // consume LoadingDocument(root) from initial Root request
            underTest.loadDocumentInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
            advanceUntilIdle()
            val state =
                awaitItem() // DocumentData(root) from loadDocumentInBackground(CLOUD_DRIVE_ROOT_ID)
            assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.DocumentData::class.java)
            val documentData = state as CloudDriveDocumentProviderUiState.DocumentData
            assertThat(documentData.accountName).isEqualTo("test@mega.co.nz")
            assertThat(documentData.documentId).isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
            assertThat(documentData.document).isEqualTo(rootRow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that loadDocumentInBackground emits LoadingDocument then DocumentData when node found`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val handle = 54321L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedNode: DefaultTypedFolderNode = mock()
            whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(mockNode)
            whenever(addNodeType.invoke(any())).thenReturn(typedNode)
            val expectedRow = CloudDriveDocumentRow(
                documentId = documentId,
                displayName = "Loaded Doc",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 1000L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(any(), any())).thenReturn(expectedRow)

            underTest.state.test {
                skipItems(1) // skip Initialising (StateFlow initial value)
                awaitItem() // consume LoadingDocument(root) from initial Root request
                awaitItem() // consume DocumentData(root) from initial Root request
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                val loadingDoc = awaitItem() // LoadingDocument(documentId)
                assertThat(loadingDoc).isInstanceOf(CloudDriveDocumentProviderUiState.LoadingDocument::class.java)
                assertThat((loadingDoc as CloudDriveDocumentProviderUiState.LoadingDocument).currentDocumentId)
                    .isEqualTo(documentId)
                val documentData = awaitItem() // DocumentData(documentId)
                assertThat(documentData).isInstanceOf(CloudDriveDocumentProviderUiState.DocumentData::class.java)
                assertThat((documentData as CloudDriveDocumentProviderUiState.DocumentData).document)
                    .isEqualTo(expectedRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(getNodeByHandleUseCase).invoke(handle, false)
            verify(cloudDriveDocumentRowMapper, times(2)).invoke(typedNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that loadDocumentInBackground emits LoadingDocument then FileNotFound when node null`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val documentId = "$DOCUMENT_ID_PREFIX:12345"
            whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(null)

            underTest.state.test {
                skipItems(1) // skip Initialising (StateFlow initial value)
                awaitItem() // consume LoadingDocument(root) from initial Root request
                awaitItem() // consume DocumentData(root) from initial Root request
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                val loadingDoc = awaitItem() // LoadingDocument(documentId)
                assertThat(loadingDoc).isInstanceOf(CloudDriveDocumentProviderUiState.LoadingDocument::class.java)
                val state = awaitItem() // FileNotFound(documentId)
                assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.FileNotFound::class.java)
                assertThat((state as CloudDriveDocumentProviderUiState.FileNotFound).documentId)
                    .isEqualTo(documentId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that loadChildrenInBackground emits LoadingChildren then ChildData when children loaded`() =
        runTest {
            val typedFolder: TypedNode = mock()
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(typedFolder) to false)
            )
            val expectedRow = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Child",
                mimeType = "application/octet-stream",
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(any(), any())).thenReturn(expectedRow)

            underTest.state.test {
                skipItems(1) // skip Initialising (StateFlow initial value)
                awaitItem() // consume LoadingDocument(root) from initial Root request
                awaitItem() // consume DocumentData(root) from initial Root request
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val loadingChildren = awaitItem() // LoadingChildren(CLOUD_DRIVE_ROOT_ID)
                assertThat(loadingChildren).isInstanceOf(CloudDriveDocumentProviderUiState.LoadingChildren::class.java)
                assertThat((loadingChildren as CloudDriveDocumentProviderUiState.LoadingChildren).currentParentDocumentId)
                    .isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                val childData = awaitItem() // ChildData(children=..., hasMore=false)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(1)
                assertThat(data.children[0]).isEqualTo(expectedRow)
                assertThat(data.parentId).isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that loadChildrenInBackground emits LoadingChildren then ChildData with empty list when no children`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getNodesByIdInChunkUseCase(any(), any()))
                .thenReturn(flowOf(emptyList<TypedNode>() to false))

            underTest.state.test {
                skipItems(1) // skip Initialising (StateFlow initial value)
                awaitItem() // consume LoadingDocument(root) from initial Root request
                awaitItem() // consume DocumentData(root) from initial Root request
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem() // consume LoadingChildren(CLOUD_DRIVE_ROOT_ID)
                val childData = awaitItem() // ChildData(children=empty, hasMore=false)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                assertThat((childData as CloudDriveDocumentProviderUiState.ChildData).children).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when showHiddenItems is true all nodes including sensitive are included in ChildData`() =
        runTest {
            val sensitiveNode: TypedNode = mock()
            val normalNode: TypedNode = mock()
            whenever(sensitiveNode.isMarkedSensitive).thenReturn(true)
            whenever(sensitiveNode.isSensitiveInherited).thenReturn(false)
            whenever(normalNode.isMarkedSensitive).thenReturn(false)
            whenever(normalNode.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(normalNode, sensitiveNode) to false)
            )
            val row1 = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Normal",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            val row2 = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:2",
                displayName = "Sensitive",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX)).thenReturn(
                row1
            )
            whenever(
                cloudDriveDocumentRowMapper.invoke(
                    sensitiveNode,
                    DOCUMENT_ID_PREFIX
                )
            ).thenReturn(row2)

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem()
                val childData = awaitItem()
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(2)
                assertThat(data.children).containsExactly(row1, row2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when isHiddenNodesEnabled is false all nodes including sensitive are included in ChildData`() =
        runTest {
            val sensitiveNode: TypedNode = mock()
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(sensitiveNode.isMarkedSensitive).thenReturn(true)
            whenever(sensitiveNode.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(sensitiveNode) to false)
            )
            val row = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Sensitive",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(
                cloudDriveDocumentRowMapper.invoke(
                    sensitiveNode,
                    DOCUMENT_ID_PREFIX
                )
            ).thenReturn(row)

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem()
                val childData = awaitItem()
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                assertThat((childData as CloudDriveDocumentProviderUiState.ChildData).children).hasSize(
                    1
                )
                assertThat(childData.children[0]).isEqualTo(row)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when showHiddenItems false and isHiddenNodesEnabled true nodes with isMarkedSensitive are filtered out`() =
        runTest {
            val sensitiveNode: TypedNode = mock()
            val normalNode: TypedNode = mock()
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(sensitiveNode.isMarkedSensitive).thenReturn(true)
            whenever(sensitiveNode.isSensitiveInherited).thenReturn(false)
            whenever(normalNode.isMarkedSensitive).thenReturn(false)
            whenever(normalNode.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(normalNode, sensitiveNode) to false)
            )
            val normalRow = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Normal",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX)).thenReturn(
                normalRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem() // LoadingChildren
                val childData = awaitItem() // ChildData (filtered from first combine emission)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(1)
                assertThat(data.children[0]).isEqualTo(normalRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper).invoke(normalNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that when showHiddenItems false and isHiddenNodesEnabled true nodes with isSensitiveInherited are filtered out`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val sensitiveInheritedNode: TypedNode = mock()
            val normalNode: TypedNode = mock()
            whenever(sensitiveInheritedNode.isMarkedSensitive).thenReturn(false)
            whenever(sensitiveInheritedNode.isSensitiveInherited).thenReturn(true)
            whenever(normalNode.isMarkedSensitive).thenReturn(false)
            whenever(normalNode.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(normalNode, sensitiveInheritedNode) to false)
            )
            val normalRow = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Normal",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX)).thenReturn(
                normalRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem() // LoadingChildren
                val childData = awaitItem() // ChildData (filtered from first combine emission)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(1)
                assertThat(data.children[0]).isEqualTo(normalRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper).invoke(normalNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that when showHiddenItems false and isHiddenNodesEnabled true both isMarkedSensitive and isSensitiveInherited are filtered out`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val markedSensitiveNode: TypedNode = mock()
            val inheritedSensitiveNode: TypedNode = mock()
            val normalNode: TypedNode = mock()
            whenever(markedSensitiveNode.isMarkedSensitive).thenReturn(true)
            whenever(markedSensitiveNode.isSensitiveInherited).thenReturn(false)
            whenever(inheritedSensitiveNode.isMarkedSensitive).thenReturn(false)
            whenever(inheritedSensitiveNode.isSensitiveInherited).thenReturn(true)
            whenever(normalNode.isMarkedSensitive).thenReturn(false)
            whenever(normalNode.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(normalNode, markedSensitiveNode, inheritedSensitiveNode) to false)
            )
            val normalRow = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "Normal",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX)).thenReturn(
                normalRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem() // LoadingChildren
                val childData = awaitItem() // ChildData (filtered from first combine emission)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(1)
                assertThat(data.children[0]).isEqualTo(normalRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, times(1)).invoke(any(), any())
        }

    @Test
    fun `test that when filtering sensitive all non-sensitive nodes are included`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val normal1: TypedNode = mock()
            val normal2: TypedNode = mock()
            whenever(normal1.isMarkedSensitive).thenReturn(false)
            whenever(normal1.isSensitiveInherited).thenReturn(false)
            whenever(normal2.isMarkedSensitive).thenReturn(false)
            whenever(normal2.isSensitiveInherited).thenReturn(false)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(normal1, normal2) to false)
            )
            val row1 = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:1",
                displayName = "A",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            val row2 = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:2",
                displayName = "B",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normal1, DOCUMENT_ID_PREFIX)).thenReturn(
                row1
            )
            whenever(cloudDriveDocumentRowMapper.invoke(normal2, DOCUMENT_ID_PREFIX)).thenReturn(
                row2
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem()
                val childData = awaitItem()
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).hasSize(2)
                assertThat(data.children).containsExactly(row1, row2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when showHiddenItems false and isHiddenNodesEnabled true all sensitive nodes yield empty ChildData`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val sensitive1: TypedNode = mock()
            val sensitive2: TypedNode = mock()
            whenever(sensitive1.isMarkedSensitive).thenReturn(true)
            whenever(sensitive1.isSensitiveInherited).thenReturn(false)
            whenever(sensitive2.isMarkedSensitive).thenReturn(false)
            whenever(sensitive2.isSensitiveInherited).thenReturn(true)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
                flowOf(listOf(sensitive1, sensitive2) to false)
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                awaitItem() // LoadingChildren
                val childData =
                    awaitItem() // ChildData (filtered from first combine emission, empty)
                assertThat(childData).isInstanceOf(CloudDriveDocumentProviderUiState.ChildData::class.java)
                val data = childData as CloudDriveDocumentProviderUiState.ChildData
                assertThat(data.children).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, times(0)).invoke(any(), any())
        }

    @Test
    fun `test that loadDocumentInBackground emits DocumentData when document is sensitive but showHiddenItems is true`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            val handle = 777L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedNode: TypedNode = mock()
            whenever(typedNode.isMarkedSensitive).thenReturn(true)
            whenever(typedNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(mockNode)
            whenever(addNodeType.invoke(any())).thenReturn(typedNode)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(true))
            val expectedRow = CloudDriveDocumentRow(
                documentId = documentId,
                displayName = "Sensitive Doc",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(typedNode, DOCUMENT_ID_PREFIX)).thenReturn(
                expectedRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                awaitItem() // LoadingDocument(documentId)
                val state = awaitItem()
                assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.DocumentData::class.java)
                assertThat((state as CloudDriveDocumentProviderUiState.DocumentData).document)
                    .isEqualTo(expectedRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, atLeastOnce()).invoke(typedNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that loadDocumentInBackground emits DocumentData when document is sensitive but isHiddenNodesEnabled is false`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            val handle = 666L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedNode: TypedNode = mock()
            whenever(typedNode.isMarkedSensitive).thenReturn(true)
            whenever(typedNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(mockNode)
            whenever(addNodeType.invoke(any())).thenReturn(typedNode)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(false))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            val expectedRow = CloudDriveDocumentRow(
                documentId = documentId,
                displayName = "Sensitive Doc",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(typedNode, DOCUMENT_ID_PREFIX)).thenReturn(
                expectedRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                awaitItem() // LoadingDocument(documentId)
                val state = awaitItem()
                assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.DocumentData::class.java)
                assertThat((state as CloudDriveDocumentProviderUiState.DocumentData).document)
                    .isEqualTo(expectedRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, atLeastOnce()).invoke(typedNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that loadDocumentInBackground emits DocumentData when document is not sensitive and hidden nodes filtering is on`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            val handle = 555L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedNode: TypedNode = mock()
            whenever(typedNode.isMarkedSensitive).thenReturn(false)
            whenever(typedNode.isSensitiveInherited).thenReturn(false)
            whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(mockNode)
            whenever(addNodeType.invoke(any())).thenReturn(typedNode)
            whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(true))
            whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(false))
            val expectedRow = CloudDriveDocumentRow(
                documentId = documentId,
                displayName = "Normal Doc",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(cloudDriveDocumentRowMapper.invoke(typedNode, DOCUMENT_ID_PREFIX)).thenReturn(
                expectedRow
            )

            underTest.state.test {
                skipItems(1)
                awaitItem()
                awaitItem()
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                awaitItem() // LoadingDocument(documentId)
                val state = awaitItem()
                assertThat(state).isInstanceOf(CloudDriveDocumentProviderUiState.DocumentData::class.java)
                assertThat((state as CloudDriveDocumentProviderUiState.DocumentData).document)
                    .isEqualTo(expectedRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, atLeastOnce()).invoke(typedNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that openDocumentFile returns file from getOpenableLocalFileForCloudDriveSafUseCase`() =
        runTest {
            val handle = 100L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedFileNode: TypedFileNode = mock()
            val localFile = File.createTempFile("saf_open", ".bin").apply { deleteOnExit() }
            whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(mockNode)
            whenever(addNodeType.invoke(mockNode)).thenReturn(typedFileNode)
            wheneverBlocking {
                getOpenableLocalFileForCloudDriveSafUseCase.invoke(typedFileNode)
            }.thenReturn(localFile)

            val result = underTest.openDocumentFile(documentId)

            assertThat(result).isEqualTo(localFile)
            verifyBlocking(getOpenableLocalFileForCloudDriveSafUseCase) { invoke(typedFileNode) }
        }

    @Test
    fun `test that openDocumentFile throws FileNotFoundException when node handle not found`() =
        runTest {
            val documentId = "$DOCUMENT_ID_PREFIX:999"
            whenever(getNodeByHandleUseCase.invoke(999L, false)).thenReturn(null)

            val error = assertThrows<FileNotFoundException> {
                underTest.openDocumentFile(documentId)
            }
            assertThat(error).hasMessageThat().contains("Node not found")
        }

    @Test
    fun `test that openDocumentFile throws FileNotFoundException when node is a folder`() =
        runTest {
            val handle = 200L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedFolderNode: TypedFolderNode = mock()
            whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(mockNode)
            whenever(addNodeType.invoke(mockNode)).thenReturn(typedFolderNode)

            val error = assertThrows<FileNotFoundException> {
                underTest.openDocumentFile(documentId)
            }
            assertThat(error).hasMessageThat().contains("Document is not a file")
        }

    @Test
    fun `test that openDocumentFile throws FileNotFoundException when document id is invalid`() =
        runTest {
            val invalidDocumentId = "invalid_id_without_prefix"

            val error = assertThrows<FileNotFoundException> {
                underTest.openDocumentFile(invalidDocumentId)
            }
            assertThat(error).hasMessageThat().contains("Invalid document id")
        }
    
    @Test
    fun `test that openDocumentFile wraps unexpected exception from getOpenableLocalFileForCloudDriveSafUseCase`() =
        runTest {
            val handle = 102L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            val typedFileNode: TypedFileNode = mock()
            whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(mockNode)
            whenever(addNodeType.invoke(mockNode)).thenReturn(typedFileNode)
            wheneverBlocking {
                getOpenableLocalFileForCloudDriveSafUseCase.invoke(typedFileNode)
            }.thenThrow(IllegalStateException("unexpected"))

            val error = assertThrows<FileNotFoundException> {
                underTest.openDocumentFile(documentId)
            }
            assertThat(error).hasMessageThat().contains("Unable to open document: $documentId")
        }

    @Test
    fun `test that openDocumentFile throws FileNotFoundException when documentIdToNodeIdMapper throws`() =
        runTest {
            val documentId = "$DOCUMENT_ID_PREFIX:1"
            whenever(documentIdToNodeIdMapper.invoke(any(), any())).thenThrow(
                IllegalStateException("mapper failed"),
            )

            val error = assertThrows<FileNotFoundException> {
                underTest.openDocumentFile(documentId)
            }
            assertThat(error).hasMessageThat().contains("Unable to open document: $documentId")
        }
}
