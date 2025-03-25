package mega.privacy.android.app.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.GetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.message.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetDocumentsFromSharedUrisUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
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
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val attachNodeUseCase = mock<AttachNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val sendChatAttachmentsUseCase = mock<SendChatAttachmentsUseCase>()
    private val getDocumentsFromSharedUrisUseCase = mock<GetDocumentsFromSharedUrisUseCase>()
    private var savedStateHandle = SavedStateHandle(mapOf())

    private fun initViewModel() {
        underTest = FileExplorerViewModel(
            ioDispatcher = StandardTestDispatcher(),
            monitorStorageStateEventUseCase = mock(),
            getCopyLatestTargetPathUseCase = getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase = getMoveLatestTargetPathUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            attachNodeUseCase = attachNodeUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            sendChatAttachmentsUseCase = sendChatAttachmentsUseCase,
            monitorAccountDetailUseCase = mock(),
            monitorShowHiddenItemsUseCase = mock(),
            getDocumentsFromSharedUrisUseCase = getDocumentsFromSharedUrisUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    fun resetMocks() {
        savedStateHandle = SavedStateHandle(mapOf())
        reset(
            getCopyLatestTargetPathUseCase,
            getMoveLatestTargetPathUseCase,
            getNodeAccessPermission,
            getFeatureFlagValueUseCase,
            attachNodeUseCase,
            getNodeByIdUseCase,
            sendChatAttachmentsUseCase,
            getDocumentsFromSharedUrisUseCase,
        )
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
        val filePaths = listOf("path1", "path2")
        val documents = filePaths.map { DocumentEntity(it, 3L, 89L, UriPath(it)) }
        val filesWithNames = filePaths.associateWith { it }

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
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(file.absolutePath to null),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
            )
        )

        initViewModel()

        underTest.uploadFile(file, parentHandle)
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
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(uri.toString() to fileName),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
            )
        )

        initViewModel()

        with(underTest) {
            setDocuments(documents)
            uploadFiles(parentHandle)
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
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(uri.toString() to renamedName),
                NodeId(parentHandle),
                waitNotificationPermissionResponseToStart = true,
            )
        )

        initViewModel()

        with(underTest) {
            setDocuments(documents)

            uploadFiles(parentHandle)
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
}