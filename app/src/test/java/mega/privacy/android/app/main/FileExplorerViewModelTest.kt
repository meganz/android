package mega.privacy.android.app.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetFolderTypeByHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetDocumentsFromSharedUrisUseCase
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLocationUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Test class for [FileExplorerViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileExplorerViewModelTest {

    private lateinit var underTest: FileExplorerViewModel

    private val getCopyLatestTargetUseCase = mock<GetCopyLatestTargetUseCase>()
    private val getMoveLatestTargetUseCase = mock<GetMoveLatestTargetUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val attachNodeUseCase = mock<AttachNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()
    private val getDocumentsFromSharedUrisUseCase = mock<GetDocumentsFromSharedUrisUseCase>()
    private var savedStateHandle = SavedStateHandle(mapOf())
    private val getFolderTypeByHandleUseCase = mock<GetFolderTypeByHandleUseCase>()
    private val getNodeLocationUseCase = mock<GetNodeLocationUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
        on { invoke() }.thenReturn(kotlinx.coroutines.flow.emptyFlow())
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()
    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val pitagTrigger = PitagTrigger.ShareFromApp

    private fun initViewModel() {
        underTest = FileExplorerViewModel(
            ioDispatcher = testDispatcher,
            monitorStorageStateEventUseCase = mock(),
            getCopyLatestTargetUseCase = getCopyLatestTargetUseCase,
            getMoveLatestTargetUseCase = getMoveLatestTargetUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            attachNodeUseCase = attachNodeUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            sendChatAttachmentsUseCase = sendChatAttachmentsUseCase,
            monitorAccountDetailUseCase = mock(),
            monitorShowHiddenItemsUseCase = mock(),
            getDocumentsFromSharedUrisUseCase = getDocumentsFromSharedUrisUseCase,
            savedStateHandle = savedStateHandle,
            getFolderTypeByHandleUseCase = getFolderTypeByHandleUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            getNodeLocationUseCase = getNodeLocationUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
            getRootNodeIdUseCase = getRootNodeIdUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        savedStateHandle = SavedStateHandle(mapOf())
        reset(
            getCopyLatestTargetUseCase,
            getMoveLatestTargetUseCase,
            getNodeAccessPermission,
            attachNodeUseCase,
            getNodeByIdUseCase,
            sendChatAttachmentsUseCase,
            getDocumentsFromSharedUrisUseCase,
            getFolderTypeByHandleUseCase,
            getNodeLocationUseCase,
            monitorNodeUpdatesUseCase,
            getFeatureFlagValueUseCase,
            getAncestorsIdsUseCase,
            getRootNodeIdUseCase,
        )
        // Set default behavior for monitorNodeUpdatesUseCase
        whenever(monitorNodeUpdatesUseCase()).thenReturn(kotlinx.coroutines.flow.emptyFlow())
    }

    /**
     * Checks if it is importing a text instead of files.
     * This is true if the action of the intent is ACTION_SEND, the type of the intent
     * is TYPE_TEXT_PLAIN and the intent does not contain EXTRA_STREAM extras.
     */

    @Test
    fun `test that an intent with action send, type plain text and no stream extra is marked as a text import`() {
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
        }

        initViewModel()

        assertThat(underTest.isImportingText(intent)).isTrue()
    }

    @Test
    fun `test that an intent with a stream extra is marked as not a text import`() {
        val bundle = mock<Bundle> {
            on { containsKey(Intent.EXTRA_STREAM) }.thenReturn(true)
        }
        val intent = mock<Intent> {
            on { action }.thenReturn(Intent.ACTION_SEND)
            on { type }.thenReturn(Constants.TYPE_TEXT_PLAIN)
            on { extras }.thenReturn(bundle)
        }

        initViewModel()

        assertThat(underTest.isImportingText(intent)).isFalse()
    }

    @Test
    fun `test that toDoAfter is invoked`() = runTest {
        val toDoAfter = mock<() -> Unit>()

        initViewModel()

        underTest.uploadFilesToChat(
            emptyList(),
            emptyList(),
            emptyList(),
            pitagTrigger = pitagTrigger,
            toDoAfter = toDoAfter,
        )

        verify(toDoAfter).invoke()
    }

    @Test
    fun `test that files are attached`() = runTest {
        val filePaths = listOf(UriPath("path1"), UriPath("path2"))
        val documents = filePaths.map { DocumentEntity(it.value, 3L, 89L, it) }
        val filesWithNames = filePaths.associateWith { it.value }

        initViewModel()

        underTest.uploadFilesToChat(
            chatIds = chatIds,
            documents = documents,
            nodeIds = emptyList(),
            pitagTrigger = pitagTrigger,
            toDoAfter = {},
        )

        verify(sendChatAttachmentsUseCase).invoke(
            filesWithNames,
            false,
            chatIds = chatIds.toLongArray(),
            pitagTrigger = pitagTrigger,
        )
    }

    @Test
    fun `test that nodes are attached`() = runTest {
        val nodeId1 = NodeId(1L)
        val nodeId2 = NodeId(2L)
        val nodeIds = listOf(nodeId1, nodeId2)
        val fileNode1 = mock<TypedFileNode>()
        val fileNode2 = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId1)) doReturn fileNode1
        whenever(getNodeByIdUseCase(nodeId2)) doReturn fileNode2

        initViewModel()

        underTest.uploadFilesToChat(
            chatIds = chatIds,
            documents = emptyList(),
            nodeIds = nodeIds,
            pitagTrigger = pitagTrigger,
            toDoAfter = {},
        )

        chatIds.forEach {
            verify(attachNodeUseCase).invoke(it, fileNode1)
            verify(attachNodeUseCase).invoke(it, fileNode2)
        }
    }

    private val chatIds = listOf(10L, 20L)

    @Test
    fun `test that state is updated correctly if upload a File`() = runTest {
        val file = File("path")
        val parentHandle = 123L
        val pitagTrigger = PitagTrigger.ShareFromApp
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(file.absolutePath to null),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
                pitagTrigger = pitagTrigger,
            )
        )

        initViewModel()

        underTest.uploadFile(file, parentHandle, pitagTrigger)
        underTest.uiState.map { it.uploadEvent }.test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that state is updated correctly if upload files without renaming`() = runTest {
        val fileName = "name"
        val uriPath = UriPath("/path/$fileName")
        val uri = mock<Uri> {
            on { toString() } doReturn uriPath.value
        }
        val documents = listOf(DocumentEntity(fileName, 656L, 454L, uriPath))
        val parentHandle = 123L
        val pitagTrigger = PitagTrigger.Scanner
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(uri.toString() to fileName),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
                pitagTrigger = pitagTrigger,
            )
        )

        initViewModel()

        with(underTest) {
            setDocuments(documents)
            uploadFiles(parentHandle, emptyList(), pitagTrigger)
            uiState.map { it.uploadEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that state is updated correctly if upload files renaming`() = runTest {
        val fileName = "name"
        val uriPath = UriPath("/path/$fileName")
        val renamedName = "newName"
        val documents =
            listOf(DocumentEntity(renamedName, 656L, 454L, uriPath, originalName = fileName))
        val uri = mock<Uri> {
            on { toString() } doReturn "/path/$fileName"
        }
        val parentHandle = 123L
        val pitagTrigger = PitagTrigger.Scanner
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(uri.toString() to renamedName),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
                pitagTrigger = pitagTrigger,
            )
        )

        initViewModel()

        with(underTest) {
            setDocuments(documents)

            uploadFiles(parentHandle, emptyList(), pitagTrigger)
            uiState.map { it.uploadEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that state is updated correctly if upload files and collided files`() = runTest {
        val fileName1 = "name1"
        val uriPath1 = UriPath("/path/$fileName1")
        val uri1 = mock<Uri> {
            on { toString() } doReturn uriPath1.value
        }
        val fileName2 = "name2"
        val uriPath2 = UriPath("/path/$fileName2")
        val documents = listOf(
            DocumentEntity(fileName1, 656L, 454L, uriPath1),
            DocumentEntity(fileName2, 656L, 454L, uriPath2)
        )
        val parentHandle = 123L
        val pitagTrigger = PitagTrigger.ShareFromApp
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(uri1.toString() to fileName1),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
                pitagTrigger = pitagTrigger,
            )
        )
        val collidedFiles = listOf(uriPath2.value)

        initViewModel()

        with(underTest) {
            setDocuments(documents)
            uploadFiles(parentHandle, collidedFiles, pitagTrigger)
            uiState.map { it.uploadEvent }.test {
                assertThat(awaitItem()).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `test that there is a scan to be uploaded`() = runTest {
        val hasMultipleScans = false
        savedStateHandle[FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS] = hasMultipleScans
        savedStateHandle[FileExplorerActivity.EXTRA_SCAN_FILE_TYPE] = 1

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.hasMultipleScans).isEqualTo(hasMultipleScans)
            assertThat(state.isUploadingScans).isTrue()
        }
    }

    @Test
    fun `test that no scans will be uploaded`() = runTest {
        val hasMultipleScans = false
        savedStateHandle[FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS] = hasMultipleScans
        savedStateHandle[FileExplorerActivity.EXTRA_SCAN_FILE_TYPE] = -1

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.hasMultipleScans).isEqualTo(hasMultipleScans)
            assertThat(state.isUploadingScans).isFalse()
        }
    }

    @Test
    fun `test that a warning dialog is shown where there are scans to be uploaded and a back navigation event occurs`() =
        runTest {
            savedStateHandle[FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS] = true
            savedStateHandle[FileExplorerActivity.EXTRA_SCAN_FILE_TYPE] = 1

            initViewModel()
            underTest.handleBackNavigation()

            underTest.uiState.test {
                assertThat(awaitItem().isScanUploadingAborted).isTrue()
            }
        }

    @Test
    fun `test that the screen is immediately exited when there are no scans to be uploaded and a back navigation event occurs`() =
        runTest {
            savedStateHandle[FileExplorerActivity.EXTRA_HAS_MULTIPLE_SCANS] = false
            savedStateHandle[FileExplorerActivity.EXTRA_SCAN_FILE_TYPE] = -1

            initViewModel()
            underTest.handleBackNavigation()

            underTest.uiState.test {
                assertThat(awaitItem().shouldFinishScreen).isTrue()
            }
        }

    @ParameterizedTest(name = "isScanUploadingAborted: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isScanUploadingAborted is updated`(isScanUploadingAborted: Boolean) = runTest {
        initViewModel()
        underTest.setIsScanUploadingAborted(isScanUploadingAborted)

        underTest.uiState.test {
            assertThat(awaitItem().isScanUploadingAborted).isEqualTo(isScanUploadingAborted)
        }
    }

    @ParameterizedTest(name = "shouldFinishScreen: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that shouldFinishScreen is updated`(shouldFinishScreen: Boolean) = runTest {
        initViewModel()
        underTest.setShouldFinishScreen(shouldFinishScreen)

        underTest.uiState.test {
            assertThat(awaitItem().shouldFinishScreen).isEqualTo(shouldFinishScreen)
        }
    }

    @ParameterizedTest(name = " when setIsAskingForCollisionsResolution: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that setIsAskingForCollisionsResolution is updated`(
        isAskingForCollisionsResolution: Boolean,
    ) = runTest {
        initViewModel()
        underTest.setIsAskingForCollisionsResolution(isAskingForCollisionsResolution)

        underTest.uiState.test {
            assertThat(awaitItem().isAskingForCollisionsResolution)
                .isEqualTo(isAskingForCollisionsResolution)
        }
    }

    @ParameterizedTest(name = " when folder type is {0}")
    @MethodSource("provideFolderType")
    fun `test that the getFolderType returns correctly`(
        folderType: FolderType,
    ) = runTest {
        val testHandle = 1234L
        whenever(getFolderTypeByHandleUseCase(testHandle)).thenReturn(folderType)

        val actual = underTest.getFolderType(testHandle)
        assertThat(actual).isEqualTo(folderType)
    }

    private fun provideFolderType() = listOf(
        Arguments.of(FolderType.Default),
        Arguments.of(FolderType.MediaSyncFolder),
        Arguments.of(FolderType.ChatFilesFolder),
        Arguments.of(FolderType.RootBackup),
        Arguments.of(FolderType.ChildBackup),
        Arguments.of(FolderType.Sync),
    )

    @Test
    fun `test that initial node updated event state is consumed`() = runTest {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        initViewModel()

        underTest.uiState.test {
            val initial = awaitItem()
            assertThat(initial.nodeUpdatedEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that node updated event is triggered when monitorNodeUpdatesUseCase emits`() =
        runTest {
            val mockNode = mock<Node>()
            val nodeChanges = listOf(NodeChanges.Name)
            val nodeUpdate = NodeUpdate(mapOf(mockNode to nodeChanges))

            whenever(monitorNodeUpdatesUseCase()).thenReturn(
                kotlinx.coroutines.flow.flowOf(
                    nodeUpdate
                )
            )
            initViewModel()
            testScheduler.advanceUntilIdle()
            assertThat(underTest.uiState.value.nodeUpdatedEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that node updated event can be consumed`() = runTest {
        val mockNode = mock<Node>()
        val nodeChanges = listOf(NodeChanges.Name, NodeChanges.Parent, NodeChanges.Attributes)
        val nodeUpdate = NodeUpdate(mapOf(mockNode to nodeChanges))

        whenever(monitorNodeUpdatesUseCase()).thenReturn(kotlinx.coroutines.flow.flowOf(nodeUpdate))
        initViewModel()
        testScheduler.advanceUntilIdle()
        assertThat(underTest.uiState.value.nodeUpdatedEvent).isEqualTo(triggered)
        underTest.consumeNodeUpdate()
        assertThat(underTest.uiState.value.nodeUpdatedEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that getFolderDestinations sets navigateToCloud event with valid handle`() = runTest {
        val handle = 123L
        val nodeId = NodeId(handle)
        val parentId = NodeId(456L)
        val ancestorId = NodeId(789L)
        val mockNode = mock<TypedNode>()
        val nodeLocation = NodeLocation(
            node = mockNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            ancestorIds = listOf(ancestorId)
        )
        val message = "Test message"

        whenever(mockNode.id).thenReturn(nodeId)
        whenever(mockNode.parentId).thenReturn(parentId)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockNode)
        whenever(getNodeLocationUseCase(mockNode)).thenReturn(nodeLocation)

        initViewModel()

        underTest.getFolderDestinations(handle, message)
        testScheduler.advanceUntilIdle()

        val navigateEvent = underTest.uiState.value.navigateToCloud
        assertThat(navigateEvent).isInstanceOf(StateEventWithContentTriggered::class.java)
        val eventContent = (navigateEvent as StateEventWithContentTriggered).content
        assertThat(eventContent.nodeId).isEqualTo(nodeId)
        assertThat(eventContent.message).isEqualTo(message)
        assertThat(eventContent.folderDestinations).isNotNull()
        assertThat(eventContent.folderDestinations?.size).isEqualTo(2) // ancestorId, handle
        assertThat(eventContent.folderDestinations?.get(0)?.nodeHandle).isEqualTo(ancestorId.longValue)
        assertThat(eventContent.folderDestinations?.get(1)?.nodeHandle).isEqualTo(handle)
    }

    @Test
    fun `test that consumeFolderDestinations consumes navigateToCloud event`() = runTest {
        val handle = 123L
        val nodeId = NodeId(handle)
        val parentId = NodeId(456L)
        val ancestorId = NodeId(789L)
        val mockNode = mock<TypedNode>()
        val nodeLocation = NodeLocation(
            node = mockNode,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            ancestorIds = listOf(ancestorId)
        )

        whenever(mockNode.id).thenReturn(nodeId)
        whenever(mockNode.parentId).thenReturn(parentId)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockNode)
        whenever(getNodeLocationUseCase(mockNode)).thenReturn(nodeLocation)

        initViewModel()

        underTest.getFolderDestinations(handle, null)
        testScheduler.advanceUntilIdle()

        // Verify event is triggered
        val navigateEvent = underTest.uiState.value.navigateToCloud
        assertThat(navigateEvent).isInstanceOf(StateEventWithContentTriggered::class.java)

        // Consume the event
        underTest.consumeFolderDestinations()

        // Verify event is consumed
        assertThat(underTest.uiState.value.navigateToCloud).isEqualTo(consumed())
    }

    @ParameterizedTest()
    @ValueSource(booleans = [true, false])
    fun `test that initCloudExplorerState updates state correctly`(
        isFeatureFlagEnabled: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn isFeatureFlagEnabled
        initViewModel()

        underTest.initCloudExplorerState(123L)
        testScheduler.advanceUntilIdle()

        underTest.uiState.test {
            assertThat(awaitItem().isFeatureFlagEnabled).isEqualTo(isFeatureFlagEnabled)
        }
    }

    @Test
    fun `test that popCloudDriveFolderForBack returns null when path is empty`() {
        initViewModel()

        assertThat(underTest.popCloudDriveFolderForBack()).isNull()
    }

    @Test
    fun `test that popCloudDriveFolderForBack returns null when path has only one element`() {
        initViewModel()
        underTest.setCloudDriveFolderPath(listOf(100L))

        assertThat(underTest.popCloudDriveFolderForBack()).isNull()
    }

    @Test
    fun `test that popCloudDriveFolderForBack returns parent handle after push`() {
        val root = 1L
        val child = 2L
        initViewModel()
        underTest.setCloudDriveFolderPath(listOf(root))
        underTest.pushCloudDriveFolder(child)

        assertThat(underTest.popCloudDriveFolderForBack()).isEqualTo(root)
    }

    @Test
    fun `test that pushCloudDriveFolder allows multi-level back navigation`() {
        val root = 1L
        val folderA = 2L
        val folderB = 3L
        initViewModel()
        underTest.setCloudDriveFolderPath(listOf(root))
        underTest.pushCloudDriveFolder(folderA)
        underTest.pushCloudDriveFolder(folderB)

        assertThat(underTest.popCloudDriveFolderForBack()).isEqualTo(folderA)
        assertThat(underTest.popCloudDriveFolderForBack()).isEqualTo(root)
        assertThat(underTest.popCloudDriveFolderForBack()).isNull()
    }

    private suspend fun initCloudRootHandle(root: Long) {
        whenever(getRootNodeIdUseCase()).thenReturn(NodeId(root))
        underTest.getOrInitCloudRootHandle()
    }

    @Test
    fun `test that getOrInitCloudRootHandle stores the handle`() = runTest(testDispatcher) {
        val root = 42L
        initViewModel()

        initCloudRootHandle(root)

        assertThat(underTest.getCloudRootHandle()).isEqualTo(root)
    }

    @Test
    fun `test that rebuildCloudDriveFolderPath sets path from use case result`() =
        runTest(testDispatcher) {
            val root = 1L
            val folder = 2L
            val node = mock<TypedNode>()
            whenever(getNodeByIdUseCase(NodeId(folder))).thenReturn(node)
            whenever(getAncestorsIdsUseCase(node)).thenReturn(listOf(NodeId(root)))
            initViewModel()
            initCloudRootHandle(root)

            underTest.rebuildCloudDriveFolderPath(folder)

            assertThat(underTest.popCloudDriveFolderForBack()).isEqualTo(root)
        }

    @Test
    fun `test that rebuildCloudDriveFolderPath replaces previously pushed entries`() =
        runTest(testDispatcher) {
            val root = 1L
            val folderA = 2L
            val staleEntry = 99L
            val node = mock<TypedNode>()
            whenever(getNodeByIdUseCase(NodeId(folderA))).thenReturn(node)
            whenever(getAncestorsIdsUseCase(node)).thenReturn(listOf(NodeId(root)))
            initViewModel()
            initCloudRootHandle(root)
            underTest.setCloudDriveFolderPath(listOf(root))
            underTest.pushCloudDriveFolder(staleEntry)

            underTest.rebuildCloudDriveFolderPath(folderA)

            // After rebuild path is [root, folderA]; staleEntry must be gone
            assertThat(underTest.popCloudDriveFolderForBack()).isEqualTo(root)
            assertThat(underTest.popCloudDriveFolderForBack()).isNull()
        }

    @Test
    fun `test that isCloudRootInitialized returns false when root handle is not set`() {
        initViewModel()

        assertThat(underTest.isCloudRootInitialized()).isFalse()
    }

    @Test
    fun `test that isCloudRootInitialized returns true after getOrInitCloudRootHandle`() =
        runTest(testDispatcher) {
            initViewModel()
            initCloudRootHandle(1L)

            assertThat(underTest.isCloudRootInitialized()).isTrue()
        }

    @Test
    fun `test that isAtCloudRoot returns false when root handle is not set`() {
        initViewModel()

        assertThat(underTest.isAtCloudRoot(1L)).isFalse()
    }

    @Test
    fun `test that isAtCloudRoot returns true when handle matches root`() = runTest(testDispatcher) {
        val root = 1L
        initViewModel()
        initCloudRootHandle(root)

        assertThat(underTest.isAtCloudRoot(root)).isTrue()
    }

    @Test
    fun `test that isAtCloudRoot returns false when handle does not match root`() =
        runTest(testDispatcher) {
            initViewModel()
            initCloudRootHandle(1L)

            assertThat(underTest.isAtCloudRoot(2L)).isFalse()
        }

    @Test
    fun `test that rebuildCloudDriveFolderPath falls back to root when node is not found`() =
        runTest(testDispatcher) {
            val root = 1L
            val folder = 99L
            whenever(getNodeByIdUseCase(NodeId(folder))).thenReturn(null)
            initViewModel()
            initCloudRootHandle(root)

            underTest.rebuildCloudDriveFolderPath(folder)

            // path = [root], at root so pop returns null
            assertThat(underTest.popCloudDriveFolderForBack()).isNull()
        }

    @Test
    fun `test that initCloudExplorerState resolves disabledTargetId from the parent of the given handle`() =
        runTest {
            val parentId = NodeId(456L)
            val mockNode = mock<TypedNode> { on { this.parentId } doReturn parentId }
            whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(mockNode)
            whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn true
            initViewModel()

            underTest.initCloudExplorerState(123L)
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().disabledTargetId).isEqualTo(parentId)
            }
        }

    @Test
    fun `test that initCloudExplorerState leaves disabledTargetId null when the handle is null`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn true
            initViewModel()

            underTest.initCloudExplorerState(null)
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().disabledTargetId).isNull()
            }
            verifyNoInteractions(getNodeByIdUseCase)
        }

    @Test
    fun `test that initCloudExplorerState does not resolve disabledTargetId when the feature flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn false
            initViewModel()

            underTest.initCloudExplorerState(123L)
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().disabledTargetId).isNull()
            }
            verifyNoInteractions(getNodeByIdUseCase)
        }

    @Test
    fun `test that initCloudExplorerState leaves disabledTargetId null when the node is not found`() =
        runTest {
            whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(null)
            whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn true
            initViewModel()

            underTest.initCloudExplorerState(123L)
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().disabledTargetId).isNull()
            }
        }

    @Test
    fun `test that initCloudExplorerState leaves disabledTargetId null when the lookup fails`() =
        runTest {
            whenever(getNodeByIdUseCase(NodeId(123L))).thenThrow(RuntimeException("boom"))
            whenever(getFeatureFlagValueUseCase(AppFeatures.CloudExplorer)) doReturn true
            initViewModel()

            underTest.initCloudExplorerState(123L)
            testScheduler.advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().disabledTargetId).isNull()
            }
        }
}
