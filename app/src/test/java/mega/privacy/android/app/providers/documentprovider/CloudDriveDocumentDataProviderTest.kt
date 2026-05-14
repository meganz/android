package mega.privacy.android.app.providers.documentprovider

import android.provider.DocumentsContract.Document
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.providers.documentprovider.model.ChildrenSlot
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveDocumentRow
import mega.privacy.android.app.providers.documentprovider.model.CloudDriveSessionState
import mega.privacy.android.app.providers.documentprovider.model.DocumentSlot
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.entity.pitag.PitagTarget
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.node.CreateFolderNodeUseCase
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.GetNodesByIdInChunkUseCase
import mega.privacy.android.domain.usecase.node.GetOpenableLocalFileForCloudDriveSafUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import dagger.Lazy as DaggerLazy

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
    private val createFolderNodeUseCase: CreateFolderNodeUseCase = mock()
    private val renameNodeUseCase: RenameNodeUseCase = mock()
    private val getChildNodeUseCase: GetChildNodeUseCase = mock()
    private val startUploadUseCase: StartUploadUseCase = mock()
    private val getCacheFileUseCase: GetCacheFileUseCase = mock()
    private val mockedCredentials: UserCredentials = mock()

    private lateinit var underTest: CloudDriveDocumentDataProvider

    private companion object {
        private const val DOCUMENT_ID_PREFIX = "mega_cloud_drive_root"
        private val ROOT_NODE_ID = NodeId(1L)
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
            createFolderNodeUseCase,
            renameNodeUseCase,
            getChildNodeUseCase,
            startUploadUseCase,
            getCacheFileUseCase,
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

    private fun <T> lazyOf(value: T): DaggerLazy<T> = DaggerLazy { value }

    private fun initUnderTest() {
        underTest = CloudDriveDocumentDataProvider(
            applicationScope = testScope,
            getRootNodeIdUseCase = lazyOf(getRootNodeIdUseCase),
            getNodesByIdInChunkUseCase = lazyOf(getNodesByIdInChunkUseCase),
            getNodeByHandleUseCase = lazyOf(getNodeByHandleUseCase),
            backgroundFastLoginUseCase = lazyOf(backgroundFastLoginUseCase),
            monitorNodeUpdatesUseCase = lazyOf(monitorNodeUpdatesUseCase),
            monitorUserCredentialsUseCase = lazyOf(monitorUserCredentialsUseCase),
            getAccountCredentialsUseCase = lazyOf(getAccountCredentialsUseCase),
            monitorHiddenNodesEnabledUseCase = lazyOf(monitorHiddenNodesEnabledUseCase),
            monitorShowHiddenItemsUseCase = lazyOf(monitorShowHiddenItemsUseCase),
            cloudDriveDocumentRowMapper = lazyOf(cloudDriveDocumentRowMapper),
            addNodeType = lazyOf(addNodeType),
            documentIdToNodeIdMapper = lazyOf(documentIdToNodeIdMapper),
            monitorPasscodeLockPreferenceUseCase = lazyOf(monitorPasscodeLockPreferenceUseCase),
            getOpenableLocalFileForCloudDriveSafUseCase = lazyOf(getOpenableLocalFileForCloudDriveSafUseCase),
            createFolderNodeUseCase = lazyOf(createFolderNodeUseCase),
            renameNodeUseCase = lazyOf(renameNodeUseCase),
            getChildNodeUseCase = lazyOf(getChildNodeUseCase),
            startUploadUseCase = lazyOf(startUploadUseCase),
            getCacheFileUseCase = lazyOf(getCacheFileUseCase),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region sessionState

    @Test
    fun `test that sessionState is NotLoggedIn when credentials null`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(null))

        underTest.sessionState.test {
            skipItems(1) // skip Initialising (StateFlow initial value)
            assertThat(awaitItem()).isInstanceOf(CloudDriveSessionState.NotLoggedIn::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sessionState is PasscodeLockEnabled when credentials exist and passcode enabled`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(monitorPasscodeLockPreferenceUseCase()).thenReturn(flowOf(true))

            underTest.sessionState.test {
                skipItems(1) // skip Initialising
                val state = awaitItem()
                assertThat(state).isInstanceOf(
                    CloudDriveSessionState.PasscodeLockEnabled::class.java
                )
                assertThat((state as CloudDriveSessionState.PasscodeLockEnabled).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sessionState is Offline when credentials exist and updateConnectivity false`() =
        runTest {
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)

            underTest.sessionState.test {
                skipItems(1) // skip Initialising
                awaitItem() // ensure we're past initial (RootNodeNotLoaded or Ready)
                underTest.updateConnectivity(false)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(CloudDriveSessionState.Offline::class.java)
                assertThat((state as CloudDriveSessionState.Offline).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sessionState is RootNodeNotLoaded when credentials exist but root node is null`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(null)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)

            underTest.sessionState.test {
                skipItems(1) // skip Initialising
                val state = awaitItem()
                assertThat(state).isInstanceOf(
                    CloudDriveSessionState.RootNodeNotLoaded::class.java
                )
                assertThat((state as CloudDriveSessionState.RootNodeNotLoaded).accountName)
                    .isEqualTo("test@mega.co.nz")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sessionState is Ready when credentials and root node available`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)

        underTest.sessionState.test {
            skipItems(1)
            val state = awaitItem()
            assertThat(state).isInstanceOf(CloudDriveSessionState.Ready::class.java)
            val ready = state as CloudDriveSessionState.Ready
            assertThat(ready.accountName).isEqualTo("test@mega.co.nz")
            assertThat(ready.rootNodeDocumentId).isEqualTo("$DOCUMENT_ID_PREFIX:${ROOT_NODE_ID.longValue}")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion sessionState

    // region documentState

    @Test
    fun `test that documentState is Idle when session is not Ready`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(null))

        underTest.documentState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(DocumentSlot.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that documentState resolves root to Loaded when session becomes Ready`() = runTest {
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

        underTest.documentState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(DocumentSlot.Loaded::class.java)
            val loaded = state as DocumentSlot.Loaded
            assertThat(loaded.documentId).isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
            assertThat(loaded.row).isEqualTo(rootRow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that loadDocumentInBackground emits Loaded when node found`() = runTest {
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

        underTest.documentState.test {
            advanceUntilIdle()
            expectMostRecentItem() // drain initial root resolution
            underTest.loadDocumentInBackground(documentId)
            advanceUntilIdle()
            val loaded = expectMostRecentItem()
            assertThat(loaded).isInstanceOf(DocumentSlot.Loaded::class.java)
            assertThat((loaded as DocumentSlot.Loaded).documentId).isEqualTo(documentId)
            assertThat(loaded.row).isEqualTo(expectedRow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that loadDocumentInBackground emits NotFound when node null`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
        val documentId = "$DOCUMENT_ID_PREFIX:12345"
        whenever(getNodeByHandleUseCase.invoke(any(), any())).thenReturn(null)

        underTest.documentState.test {
            advanceUntilIdle()
            expectMostRecentItem() // drain initial root NotFound
            underTest.loadDocumentInBackground(documentId)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(DocumentSlot.NotFound::class.java)
            assertThat((state as DocumentSlot.NotFound).documentId).isEqualTo(documentId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion documentState

    // region childrenState

    @Test
    fun `test that childrenState is Idle when session is not Ready`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)
        whenever(monitorUserCredentialsUseCase()).thenReturn(flowOf(null))

        underTest.childrenState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(ChildrenSlot.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that loadChildrenInBackground emits Loaded with children when load succeeds`() =
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

            underTest.childrenState.test {
                skipItems(1) // Idle
                awaitItem() // initial children for root (NotFound — root unmocked as TypedNode)
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                val loaded = state as ChildrenSlot.Loaded
                assertThat(loaded.children).hasSize(1)
                assertThat(loaded.children[0]).isEqualTo(expectedRow)
                assertThat(loaded.parentDocumentId)
                    .isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that loadChildrenInBackground emits Loaded with empty list when no children`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            whenever(getNodesByIdInChunkUseCase(any(), any()))
                .thenReturn(flowOf(emptyList<TypedNode>() to false))

            underTest.childrenState.test {
                skipItems(1) // Idle
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                assertThat((state as ChildrenSlot.Loaded).children).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when showHiddenItems is true all nodes including sensitive are included in childrenState`() =
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
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX))
                .thenReturn(row1)
            whenever(cloudDriveDocumentRowMapper.invoke(sensitiveNode, DOCUMENT_ID_PREFIX))
                .thenReturn(row2)

            underTest.childrenState.test {
                skipItems(1)
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                val loaded = state as ChildrenSlot.Loaded
                assertThat(loaded.children).hasSize(2)
                assertThat(loaded.children).containsExactly(row1, row2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when isHiddenNodesEnabled is false all nodes including sensitive are included`() =
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
            whenever(cloudDriveDocumentRowMapper.invoke(sensitiveNode, DOCUMENT_ID_PREFIX))
                .thenReturn(row)

            underTest.childrenState.test {
                skipItems(1)
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                val loaded = state as ChildrenSlot.Loaded
                assertThat(loaded.children).hasSize(1)
                assertThat(loaded.children[0]).isEqualTo(row)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sensitive nodes are filtered out when showHiddenItems false and isHiddenNodesEnabled true`() =
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
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX))
                .thenReturn(normalRow)

            underTest.childrenState.test {
                skipItems(1)
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                val loaded = state as ChildrenSlot.Loaded
                assertThat(loaded.children).hasSize(1)
                assertThat(loaded.children[0]).isEqualTo(normalRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper).invoke(normalNode, DOCUMENT_ID_PREFIX)
        }

    @Test
    fun `test that inherited-sensitive nodes are filtered out when showHiddenItems false and isHiddenNodesEnabled true`() =
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
            whenever(cloudDriveDocumentRowMapper.invoke(normalNode, DOCUMENT_ID_PREFIX))
                .thenReturn(normalRow)

            underTest.childrenState.test {
                skipItems(1)
                awaitItem()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
                val loaded = state as ChildrenSlot.Loaded
                assertThat(loaded.children).hasSize(1)
                assertThat(loaded.children[0]).isEqualTo(normalRow)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that documentState emits Loaded when document is sensitive but showHiddenItems is true`() =
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
            whenever(cloudDriveDocumentRowMapper.invoke(typedNode, DOCUMENT_ID_PREFIX))
                .thenReturn(expectedRow)

            underTest.documentState.test {
                advanceUntilIdle()
                expectMostRecentItem() // drain initial root resolution
                underTest.loadDocumentInBackground(documentId)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state).isInstanceOf(DocumentSlot.Loaded::class.java)
                assertThat((state as DocumentSlot.Loaded).documentId).isEqualTo(documentId)
                assertThat(state.row).isEqualTo(expectedRow)
                cancelAndIgnoreRemainingEvents()
            }
            verify(cloudDriveDocumentRowMapper, atLeastOnce()).invoke(typedNode, DOCUMENT_ID_PREFIX)
        }

    // endregion childrenState

    // region findCachedChildRow

    @Test
    fun `test that findCachedChildRow returns null when nothing has been loaded`() {
        assertThat(underTest.findCachedChildRow("$DOCUMENT_ID_PREFIX:1")).isNull()
    }

    @Test
    fun `test that findCachedChildRow returns row from currently loaded childrenState`() = runTest {
        val typedFolder: TypedNode = mock()
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
        whenever(getNodesByIdInChunkUseCase(any(), any())).thenReturn(
            flowOf(listOf(typedFolder) to false)
        )
        val expectedRow = CloudDriveDocumentRow(
            documentId = "$DOCUMENT_ID_PREFIX:42",
            displayName = "Child",
            mimeType = Document.MIME_TYPE_DIR,
            size = 0L,
            lastModified = 0L,
            flags = 0,
        )
        whenever(cloudDriveDocumentRowMapper.invoke(any(), any())).thenReturn(expectedRow)

        underTest.childrenState.test {
            advanceUntilIdle()
            underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state).isInstanceOf(ChildrenSlot.Loaded::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(underTest.findCachedChildRow("$DOCUMENT_ID_PREFIX:42"))
            .isEqualTo(expectedRow)
    }

    @Test
    fun `test that findCachedChildRow survives childrenState transitions`() =
        runTest {
            // AND-23569: Get Info should resolve a child's row even after the parent listing
            // has changed. recentDocumentRows preserves the row across childrenState transitions.
            val typedFolder: TypedNode = mock()
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getAccountCredentialsUseCase()).thenReturn(mockedCredentials)
            val cachedRow = CloudDriveDocumentRow(
                documentId = "$DOCUMENT_ID_PREFIX:200",
                displayName = "First",
                mimeType = Document.MIME_TYPE_DIR,
                size = 0L,
                lastModified = 0L,
                flags = 0,
            )
            whenever(getNodesByIdInChunkUseCase(any(), any()))
                .thenReturn(flowOf(listOf(typedFolder) to false))
            whenever(cloudDriveDocumentRowMapper.invoke(any(), any())).thenReturn(cachedRow)

            // Step 1: load root children so :200 is cached.
            underTest.childrenState.test {
                advanceUntilIdle()
                underTest.loadChildrenInBackground(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(underTest.findCachedChildRow("$DOCUMENT_ID_PREFIX:200"))
                .isEqualTo(cachedRow)

            // Step 2: switch to a parent that resolves to NotFound — childrenState transitions
            // away from the root's Loaded, but the row from the earlier listing must remain.
            underTest.childrenState.test {
                advanceUntilIdle()
                underTest.loadChildrenInBackground("$DOCUMENT_ID_PREFIX:99")
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            // The currently-loaded children no longer include :200, but the cache does.
            assertThat(underTest.findCachedChildRow("$DOCUMENT_ID_PREFIX:200"))
                .isEqualTo(cachedRow)
        }

    // endregion findCachedChildRow

    // region openDocumentFile

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

    // endregion openDocumentFile

    // region pending folder / file

    @Test
    fun `test that registerPendingFolder returns id with PENDING_PREFIX`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        val parentNodeId = NodeId(99L)
        val parentDocumentId = "$DOCUMENT_ID_PREFIX:99"
        whenever(getChildNodeUseCase(parentNodeId, "NewFolder")).thenReturn(null)

        val result = underTest.registerPendingFolder(parentDocumentId, "NewFolder")

        assertThat(result).startsWith("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:")
    }

    @Test
    fun `test that registerPendingFolder resolves root document id to root node id`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getChildNodeUseCase(ROOT_NODE_ID, "Top")).thenReturn(null)

        val result = underTest.registerPendingFolder(
            CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
            "Top",
        )

        assertThat(result).startsWith("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:")
    }

    @Test
    fun `test that registerPendingFolder rejects duplicate name with FileNotFoundException`() =
        runTest {
            val parentNodeId = NodeId(7L)
            whenever(getChildNodeUseCase(parentNodeId, "Dup")).thenReturn(mock<FileNode>())

            assertThrows<FileNotFoundException> {
                underTest.registerPendingFolder("$DOCUMENT_ID_PREFIX:7", "Dup")
            }
        }

    @Test
    fun `test that registerPendingFolder throws FileNotFoundException when parent cannot be resolved`() =
        runTest {
            assertThrows<FileNotFoundException> {
                underTest.registerPendingFolder("invalid_doc_id", "X")
            }
        }

    @Test
    fun `test that completeFolderCreation calls createFolderNodeUseCase and keeps placeholder row queryable`() =
        runTest {
            val parentNodeId = NodeId(42L)
            val createdNodeId = NodeId(420L)
            whenever(getChildNodeUseCase(parentNodeId, "F")).thenReturn(null)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            wheneverBlocking { createFolderNodeUseCase("F", parentNodeId) }
                .thenReturn(createdNodeId)
            val pendingId = underTest.registerPendingFolder("$DOCUMENT_ID_PREFIX:42", "F")

            underTest.completeFolderCreation(pendingId)

            verifyBlocking(createFolderNodeUseCase) { invoke("F", parentNodeId) }
            val row = underTest.getPendingDocumentRow(pendingId)
            assertThat(row).isNotNull()
            assertThat(row!!.documentId).isEqualTo(pendingId)
            assertThat(row.mimeType).isEqualTo(Document.MIME_TYPE_DIR)
        }

    @Test
    fun `test that completeFolderCreation clears pending entry when SDK throws`() = runTest {
        val parentNodeId = NodeId(43L)
        whenever(getChildNodeUseCase(parentNodeId, "F")).thenReturn(null)
        wheneverBlocking { createFolderNodeUseCase(any(), any()) }
            .thenAnswer { throw IllegalStateException("sdk") }
        val pendingId = underTest.registerPendingFolder("$DOCUMENT_ID_PREFIX:43", "F")

        val result = runCatching { underTest.completeFolderCreation(pendingId) }
        assertThat(result.isFailure).isTrue()
        assertThat(underTest.getPendingDocumentRow(pendingId)).isNull()
    }

    @Test
    fun `test that completeFolderCreation throws when pending id is unknown`() = runTest {
        assertThrows<IllegalStateException> {
            underTest.completeFolderCreation("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:nope")
        }
    }

    @Test
    fun `test that registerPendingFile resolves placeholder folder parent to its real node id`() =
        runTest {
            val outerParentNodeId = NodeId(50L)
            val createdFolderNodeId = NodeId(500L)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getChildNodeUseCase(outerParentNodeId, "MyFolder")).thenReturn(null)
            wheneverBlocking { createFolderNodeUseCase("MyFolder", outerParentNodeId) }
                .thenReturn(createdFolderNodeId)
            val folderPlaceholderId = underTest.registerPendingFolder(
                "$DOCUMENT_ID_PREFIX:50",
                "MyFolder",
            )
            underTest.completeFolderCreation(folderPlaceholderId)
            whenever(getChildNodeUseCase(createdFolderNodeId, "child.txt")).thenReturn(null)

            val filePlaceholderId = underTest.registerPendingFile(
                folderPlaceholderId,
                "child.txt",
                "text/plain",
            )

            assertThat(filePlaceholderId)
                .startsWith("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:")
            verifyBlocking(getChildNodeUseCase) { invoke(createdFolderNodeId, "child.txt") }
        }

    @Test
    fun `test that registerPendingFile awaits placeholder folder still being created`() = runTest {
        val outerParentNodeId = NodeId(60L)
        val createdFolderNodeId = NodeId(600L)
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getChildNodeUseCase(outerParentNodeId, "Outer")).thenReturn(null)
        whenever(getChildNodeUseCase(createdFolderNodeId, "child.bin")).thenReturn(null)
        wheneverBlocking { createFolderNodeUseCase("Outer", outerParentNodeId) }
            .thenReturn(createdFolderNodeId)
        val folderPlaceholderId = underTest.registerPendingFolder(
            "$DOCUMENT_ID_PREFIX:60",
            "Outer",
        )

        val deferredChild = async {
            underTest.registerPendingFile(
                folderPlaceholderId,
                "child.bin",
                "application/octet-stream",
            )
        }
        runCurrent()
        assertThat(deferredChild.isCompleted).isFalse()

        underTest.completeFolderCreation(folderPlaceholderId)

        val filePlaceholderId = deferredChild.await()
        assertThat(filePlaceholderId)
            .startsWith("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:")
        verifyBlocking(getChildNodeUseCase) { invoke(createdFolderNodeId, "child.bin") }
    }

    @Test
    fun `test that registerPendingFile fails fast when placeholder folder creation fails`() =
        runTest {
            val outerParentNodeId = NodeId(70L)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getChildNodeUseCase(outerParentNodeId, "Bad")).thenReturn(null)
            wheneverBlocking { createFolderNodeUseCase("Bad", outerParentNodeId) }
                .thenAnswer { throw IllegalStateException("sdk down") }
            val folderPlaceholderId = underTest.registerPendingFolder(
                "$DOCUMENT_ID_PREFIX:70",
                "Bad",
            )

            val deferredChild = async {
                runCatching {
                    underTest.registerPendingFile(folderPlaceholderId, "x.txt", "text/plain")
                }
            }
            runCurrent()
            assertThat(deferredChild.isCompleted).isFalse()

            runCatching { underTest.completeFolderCreation(folderPlaceholderId) }

            val result = deferredChild.await()
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull())
                .isInstanceOf(FileNotFoundException::class.java)
        }

    @Test
    fun `test that registerPendingFile returns id with PENDING_PREFIX`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getChildNodeUseCase(ROOT_NODE_ID, "doc.txt")).thenReturn(null)

        val result = underTest.registerPendingFile(
            CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
            "doc.txt",
            "text/plain",
        )

        assertThat(result).startsWith("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:")
    }

    @Test
    fun `test that registerPendingFile rejects duplicate name`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getChildNodeUseCase(ROOT_NODE_ID, "doc.txt")).thenReturn(mock<FileNode>())

        assertThrows<FileNotFoundException> {
            underTest.registerPendingFile(
                CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
                "doc.txt",
                "text/plain",
            )
        }
    }

    @Test
    fun `test that isPendingDocumentId is true only for pending prefix`() {
        assertThat(underTest.isPendingDocumentId("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:abc"))
            .isTrue()
        assertThat(underTest.isPendingDocumentId("$DOCUMENT_ID_PREFIX:1")).isFalse()
        assertThat(underTest.isPendingDocumentId(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID))
            .isFalse()
    }

    @Test
    fun `test that getPendingDocumentRow returns row matching the registered pending file`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getChildNodeUseCase(ROOT_NODE_ID, "a.bin")).thenReturn(null)

            val pendingId = underTest.registerPendingFile(
                CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
                "a.bin",
                "application/octet-stream",
            )
            val row = underTest.getPendingDocumentRow(pendingId)

            assertThat(row).isNotNull()
            assertThat(row!!.documentId).isEqualTo(pendingId)
            assertThat(row.displayName).isEqualTo("a.bin")
            assertThat(row.mimeType).isEqualTo("application/octet-stream")
        }

    @Test
    fun `test that getPendingDocumentRow returns null for unknown id`() {
        assertThat(underTest.getPendingDocumentRow("$DOCUMENT_ID_PREFIX:1")).isNull()
        assertThat(
            underTest.getPendingDocumentRow("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:unknown")
        ).isNull()
    }

    // endregion pending folder / file

    // region scratch file / upload

    @Test
    fun `test that prepareWriteScratchFile returns existing file from cache`() = runTest {
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getChildNodeUseCase(ROOT_NODE_ID, "scratch.txt")).thenReturn(null)
        val tempFile = File.createTempFile("saf_test_", ".tmp").apply { deleteOnExit() }
        whenever(
            getCacheFileUseCase(
                eq(CloudDriveDocumentDataProvider.SAF_UPLOADS_CACHE_FOLDER),
                any(),
            )
        ).thenReturn(tempFile)

        val pendingId = underTest.registerPendingFile(
            CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
            "scratch.txt",
            "text/plain",
        )

        val result = underTest.prepareWriteScratchFile(pendingId)

        assertThat(result).isEqualTo(tempFile)
        assertThat(result.exists()).isTrue()
    }

    @Test
    fun `test that prepareWriteScratchFile throws when documentId is unknown`() = runTest {
        assertThrows<FileNotFoundException> {
            underTest.prepareWriteScratchFile("${CloudDriveDocumentDataProvider.PENDING_PREFIX}:nope")
        }
    }

    @Test
    fun `test that onWriteScratchClosed kicks off upload via startUploadUseCase on success`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getChildNodeUseCase(ROOT_NODE_ID, "f.bin")).thenReturn(null)
            val tempFile = File.createTempFile("saf_close_", ".tmp").apply { deleteOnExit() }
            whenever(
                getCacheFileUseCase(
                    eq(CloudDriveDocumentDataProvider.SAF_UPLOADS_CACHE_FOLDER),
                    any(),
                )
            ).thenReturn(tempFile)
            whenever(
                startUploadUseCase(
                    localPath = any(),
                    parentNodeId = any(),
                    fileName = any(),
                    modificationTime = any(),
                    appData = anyOrNull(),
                    isSourceTemporary = any(),
                    shouldStartFirst = any(),
                    pitagTrigger = any(),
                    pitagTarget = any(),
                )
            ).thenReturn(emptyFlow())
            val pendingId = underTest.registerPendingFile(
                CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
                "f.bin",
                "application/octet-stream",
            )
            underTest.prepareWriteScratchFile(pendingId)

            underTest.onWriteScratchClosed(pendingId, tempFile, err = null)
            advanceUntilIdle()

            verify(startUploadUseCase).invoke(
                localPath = tempFile.absolutePath,
                parentNodeId = ROOT_NODE_ID,
                fileName = "f.bin",
                modificationTime = tempFile.lastModified() / 1000,
                appData = null,
                isSourceTemporary = true,
                shouldStartFirst = false,
                pitagTrigger = PitagTrigger.NotApplicable,
                pitagTarget = PitagTarget.CloudDrive,
            )
        }

    @Test
    fun `test that onWriteScratchClosed deletes file and skips upload when err is non null`() =
        runTest {
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getChildNodeUseCase(ROOT_NODE_ID, "f.bin")).thenReturn(null)
            val tempFile = File.createTempFile("saf_err_", ".tmp")
            whenever(
                getCacheFileUseCase(
                    eq(CloudDriveDocumentDataProvider.SAF_UPLOADS_CACHE_FOLDER),
                    any(),
                )
            ).thenReturn(tempFile)
            val pendingId = underTest.registerPendingFile(
                CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID,
                "f.bin",
                "application/octet-stream",
            )

            underTest.onWriteScratchClosed(pendingId, tempFile, err = IOException("disk full"))
            advanceUntilIdle()

            assertThat(tempFile.exists()).isFalse()
            verify(startUploadUseCase, never()).invoke(
                localPath = any(),
                parentNodeId = any(),
                fileName = any(),
                modificationTime = any(),
                appData = anyOrNull(),
                isSourceTemporary = any(),
                shouldStartFirst = any(),
                pitagTrigger = any(),
                pitagTarget = any(),
            )
        }

    // endregion scratch file / upload

    // region renameDocument

    @Test
    fun `test that renameDocument calls renameNodeUseCase and returns root document id when parent is root`() =
        runTest {
            val handle = 9999L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            val mockNode: FolderNode = mock()
            whenever(mockNode.parentId).thenReturn(ROOT_NODE_ID)
            whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
            whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(mockNode)

            val result = underTest.renameDocument(documentId, "new.txt")

            assertThat(result).isEqualTo(CloudDriveDocumentDataProvider.CLOUD_DRIVE_ROOT_ID)
            verifyBlocking(renameNodeUseCase) { invoke(handle, "new.txt") }
        }

    @Test
    fun `test that renameDocument returns parent document id when parent is not root`() = runTest {
        val handle = 9999L
        val parentHandle = 5000L
        val documentId = "$DOCUMENT_ID_PREFIX:$handle"
        val mockNode: FolderNode = mock()
        whenever(mockNode.parentId).thenReturn(NodeId(parentHandle))
        whenever(getRootNodeIdUseCase()).thenReturn(ROOT_NODE_ID)
        whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(mockNode)

        val result = underTest.renameDocument(documentId, "new.txt")

        assertThat(result).isEqualTo("$DOCUMENT_ID_PREFIX:$parentHandle")
        verifyBlocking(renameNodeUseCase) { invoke(handle, "new.txt") }
    }

    @Test
    fun `test that renameDocument throws FileNotFoundException for invalid id`() = runTest {
        assertThrows<FileNotFoundException> {
            underTest.renameDocument("not_a_valid_id", "x")
        }
    }

    @Test
    fun `test that renameDocument throws FileNotFoundException when parent cannot be resolved`() =
        runTest {
            val handle = 9999L
            val documentId = "$DOCUMENT_ID_PREFIX:$handle"
            whenever(getNodeByHandleUseCase.invoke(handle, false)).thenReturn(null)

            assertThrows<FileNotFoundException> {
                underTest.renameDocument(documentId, "x")
            }
        }

    // endregion renameDocument
}
