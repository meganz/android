package mega.privacy.android.app.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.fileexplorer.model.FileExplorerUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetFolderTypeByHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.file.GetDocumentsFromSharedUrisUseCase
import mega.privacy.android.domain.usecase.node.GetNodeLocationUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.navigation.destination.CloudDriveNavKey
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
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Test class for [FileExplorerViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FileExplorerViewModelTest {

    private lateinit var underTest: FileExplorerViewModel

    private val getCopyLatestTargetPathUseCase = mock<GetCopyLatestTargetPathUseCase>()
    private val getMoveLatestTargetPathUseCase = mock<GetMoveLatestTargetPathUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val attachNodeUseCase = mock<AttachNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()
    private val getDocumentsFromSharedUrisUseCase = mock<GetDocumentsFromSharedUrisUseCase>()
    private var savedStateHandle = SavedStateHandle(mapOf())
    private val getFolderTypeByHandleUseCase = mock<GetFolderTypeByHandleUseCase>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
        on { invoke() }.thenReturn(kotlinx.coroutines.flow.emptyFlow())
    }
    private val getNodeLocationUseCase = mock<GetNodeLocationUseCase>()
    private val testDispatcher = StandardTestDispatcher()

    private fun initViewModel() {
        underTest = FileExplorerViewModel(
            ioDispatcher = testDispatcher,
            monitorStorageStateEventUseCase = mock(),
            getCopyLatestTargetPathUseCase = getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase = getMoveLatestTargetPathUseCase,
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
        )
    }

    @BeforeEach
    fun resetMocks() {
        savedStateHandle = SavedStateHandle(mapOf())
        reset(
            getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase,
            getNodeAccessPermission,
            attachNodeUseCase,
            getNodeByIdUseCase,
            sendChatAttachmentsUseCase,
            getDocumentsFromSharedUrisUseCase,
            getFolderTypeByHandleUseCase,
            monitorNodeUpdatesUseCase,
            getNodeLocationUseCase
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
            toDoAfter = toDoAfter
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
            toDoAfter = {},
        )

        verify(sendChatAttachmentsUseCase).invoke(
            filesWithNames,
            false,
            chatIds = chatIds.toLongArray()
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
    fun `test that navigateToCloud event is triggered and consumed`() = runTest {
        val handle = 123L
        val message = "Test message"
        val nodeId = NodeId(handle)
        val mockNode = mock<TypedFileNode> {
            on { id } doReturn nodeId
            on { parentId } doReturn NodeId(456L)
        }
        val ancestorIds = listOf(NodeId(200L), NodeId(300L))
        val nodeLocation = NodeLocation(
            node = mockNode,
            nodeSourceType = mega.privacy.android.domain.entity.node.NodeSourceType.CLOUD_DRIVE,
            ancestorIds = ancestorIds
        )
        val folderDestinations = ancestorIds.plus(nodeId).map { folderId ->
            CloudDriveNavKey(nodeHandle = folderId.longValue)
        }

        whenever(getNodeByIdUseCase(nodeId)).thenReturn(mockNode)
        whenever(getNodeLocationUseCase(mockNode)).thenReturn(nodeLocation)

        underTest.getFolderDestinations(handle, message)

        assertThat(underTest.uiState.value.navigateToCloud).isEqualTo(
            triggered(
                FileExplorerUiState.NavigateToCloudEvent(
                    nodeId,
                    folderDestinations,
                    message
                )
            )
        )
        underTest.consumeFolderDestinations()

        assertThat(underTest.uiState.value.navigateToCloud).isEqualTo(consumed())
    }

}