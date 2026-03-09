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
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase = mock()
    private val monitorUserCredentialsUseCase: MonitorUserCredentialsUseCase = mock()
    private val cloudDriveDocumentRowMapper: CloudDriveDocumentRowMapper = mock()
    private val addNodeType: AddNodeType = mock()
    private val documentIdToNodeIdMapper: DocumentIdToNodeIdMapper = mock()
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
            monitorNodeUpdatesUseCase,
            monitorUserCredentialsUseCase,
            cloudDriveDocumentRowMapper,
            addNodeType,
            documentIdToNodeIdMapper,
            mockedCredentials,
        )
        whenever(monitorNodeUpdatesUseCase()).thenReturn(emptyFlow())
        whenever(mockedCredentials.email).thenReturn("test@mega.co.nz")
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(mockedCredentials))
        whenever(documentIdToNodeIdMapper.invoke(any(), any())).thenAnswer { invocation ->
            val docId = invocation.getArgument<String>(0)
            val prefix = invocation.getArgument<String>(1)
            if (!docId.startsWith("$prefix:")) null
            else docId.substring(prefix.length + 1).toLongOrNull()?.let { NodeId(it) }
        }
        underTest = CloudDriveDocumentDataProvider(
            applicationScope = testScope,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
            getNodesByIdInChunkUseCase = getNodesByIdInChunkUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            backgroundFastLoginUseCase = backgroundFastLoginUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
            cloudDriveDocumentRowMapper = cloudDriveDocumentRowMapper,
            addNodeType = addNodeType,
            documentIdToNodeIdMapper = documentIdToNodeIdMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that state is NotLoggedIn when credentials null`() = runTest {
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(null))

        underTest.state.test {
            skipItems(1) // skip Initialising (StateFlow initial value)
            assertThat(awaitItem()).isInstanceOf(CloudDriveDocumentProviderUiState.NotLoggedIn::class.java) // NotLoggedIn
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that state is DocumentData for root when credentials and root available`() = runTest {
        val mockNode: FolderNode = mock()
        val typedNode: DefaultTypedFolderNode = mock()
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
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
            val state = awaitItem() // DocumentData(root) from loadDocumentInBackground(CLOUD_DRIVE_ROOT_ID)
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
                awaitItem() // skip initial ChildData(empty, hasMore=true) from runningFold
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

}
