package mega.privacy.android.app.textEditor

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.data.extensions.observeOnce
import mega.privacy.android.app.presentation.myaccount.InstantTaskExecutorExtension
import mega.privacy.android.app.textEditor.TextEditorViewModel.Companion.MODE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.favourites.IsAvailableOfflineUseCase
import mega.privacy.android.domain.usecase.node.CheckChatNodesNameCollisionAndCopyUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionWithActionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInBackupsUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodeUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File


@ExperimentalCoroutinesApi
@ExtendWith(InstantTaskExecutorExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorViewModelTest {
    private lateinit var underTest: TextEditorViewModel

    private val checkNodesNameCollisionWithActionUseCase =
        mock<CheckNodesNameCollisionWithActionUseCase>()
    private val checkChatNodesNameCollisionAndCopyUseCase =
        mock<CheckChatNodesNameCollisionAndCopyUseCase>()
    private val isAvailableOfflineUseCase = mock<IsAvailableOfflineUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(flowOf(AccountDetail()))
    }
    private val monitorNodeUpdatesFakeFlow = MutableSharedFlow<NodeUpdate>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase> {
        on { invoke() }.thenReturn(monitorNodeUpdatesFakeFlow)
    }
    private val isHiddenNodesOnboardedUseCase = mock<IsHiddenNodesOnboardedUseCase> {
        onBlocking {
            invoke()
        }.thenReturn(false)
    }
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase> {
        onBlocking {
            invoke(any())
        }.thenReturn(false)
    }
    private val savedStateHandle = mock<SavedStateHandle>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val megaApi = mock<MegaApiAndroid>()
    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val downloadNodeUseCase = mock<DownloadNodeUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    // New mocks for smart filename generation
    private val getNodeNameCollisionRenameNameUseCase =
        mock<GetNodeNameCollisionRenameNameUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = TextEditorViewModel(
            megaApi = megaApi,
            megaApiFolder = mock(),
            megaChatApi = mock(),
            checkFileNameCollisionsUseCase = mock(),
            checkNodesNameCollisionWithActionUseCase = checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase = checkChatNodesNameCollisionAndCopyUseCase,
            getCacheFileUseCase = getCacheFileUseCase,
            downloadNodeUseCase = downloadNodeUseCase,
            ioDispatcher = StandardTestDispatcher(),
            getNodeByIdUseCase = getNodeByIdUseCase,
            getChatFileUseCase = getChatFileUseCase,
            getPublicChildNodeFromIdUseCase = mock(),
            getPublicNodeFromSerializedDataUseCase = mock(),
            updateNodeSensitiveUseCase = mock(),
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            isHiddenNodesOnboardedUseCase = isHiddenNodesOnboardedUseCase,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            isAvailableOfflineUseCase = isAvailableOfflineUseCase,
            isNodeInBackupsUseCase = isNodeInBackupsUseCase,
            savedStateHandle = savedStateHandle,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            crashReporter = mock(),
            getNodeNameCollisionRenameNameUseCase = getNodeNameCollisionRenameNameUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkNodesNameCollisionWithActionUseCase,
            checkChatNodesNameCollisionAndCopyUseCase,
            megaApi,
            getCacheFileUseCase,
            downloadNodeUseCase,
            getNodeByIdUseCase,
            getNodeNameCollisionRenameNameUseCase,
        )
    }

    @Test
    internal fun `test that copy complete snack bar message is shown when node is copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.context_correctly_copied)
        }

    @Test
    internal fun `test that copy error snack bar message is shown when node is not copied to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )

            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            testScheduler.advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.context_no_copied)
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when copy failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Copy node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.COPY,
                )
            ).thenThrow(runtimeException)
            underTest.copyNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that move complete snack bar message is shown when node is moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_moved)
            }
        }

    @Test
    internal fun `test that move error snack bar message is shown when node is not moved to different directory`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_moved)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when move failed`() =
        runTest {
            val selectedNode = 73248538798194
            val newParentNode = 158401030174851
            val runtimeException = RuntimeException("Move node failed")
            whenever(
                checkNodesNameCollisionWithActionUseCase(
                    nodes = mapOf(selectedNode to newParentNode),
                    type = NodeNameCollisionType.MOVE,
                )
            ).thenThrow(runtimeException)

            underTest.moveNode(
                nodeHandle = selectedNode,
                newParentHandle = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue(runtimeException)
        }

    @Test
    internal fun `test that copy complete snack bar message is shown when chat node is imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 0
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_correctly_copied)
            }
        }


    @Test
    internal fun `test that copy error snack bar message is shown when chat node is not imported to different directory`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ) doReturn NodeNameCollisionWithActionResult(
                collisionResult = mock(),
                moveRequestResult = MoveRequestResult.GeneralMovement(
                    count = 1,
                    errorCount = 1
                )
            )
            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()
            underTest.onSnackBarMessage().observeOnce {
                assertThat(it).isEqualTo(R.string.context_no_copied)
            }
        }

    @Test
    internal fun `test that onExceptionThrown is triggered when import failed`() =
        runTest {
            val newParentNode = NodeId(158401030174851)
            val chatId = 1000L
            val messageId = 2000L

            val runtimeException = RuntimeException("Import node failed")
            whenever(
                checkChatNodesNameCollisionAndCopyUseCase(
                    chatId = chatId,
                    messageIds = listOf(messageId),
                    newNodeParent = newParentNode,
                )
            ).thenThrow(runtimeException)

            underTest.importChatNode(
                chatId = chatId,
                messageId = messageId,
                newParentNode = newParentNode,
            )
            advanceUntilIdle()

            underTest.onExceptionThrown().observeOnce {
                assertThat(it).isEqualTo(runtimeException)
            }
        }

    @Test
    internal fun `test that snackbar message is shown when chat file is already available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(true)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onSnackBarMessage().test().assertValue(R.string.file_already_exists)
        }

    @Test
    internal fun `test that startChatFileOfflineDownload event is triggered when chat file is not available offline`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            val chatFile = mock<ChatDefaultFile>()
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
            whenever(isAvailableOfflineUseCase(chatFile)).thenReturn(false)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.uiState.test {
                val actual = awaitItem()
                val event = actual.transferEvent
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                val content = (event as StateEventWithContentTriggered).content
                assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            }
        }

    @Test
    internal fun `test that exception is handled correctly when chat file is not found`() =
        runTest {
            val chatId = 1000L
            val messageId = 2000L
            whenever(getChatFileUseCase(chatId, messageId)).thenReturn(null)

            underTest.saveChatNodeToOffline(chatId, messageId)
            advanceUntilIdle()

            underTest.onExceptionThrown().test().assertValue {
                it is IllegalStateException
            }
        }

    @ParameterizedTest(name = "when the file name is {0}")
    @MethodSource("provideIsMarkDownFileTestData")
    fun `test the isMarkDownFile is updated correctly`(fileName: String?, expectedResult: Boolean) =
        runTest {
            val testIntent = mock<Intent> {
                on { getStringExtra(INTENT_EXTRA_KEY_FILE_NAME) }.thenReturn(fileName)
            }

            underTest.setInitialValues(testIntent, mock())
            underTest.uiState.test {
                val actual = awaitItem()
                assertThat(actual.isMarkDownFile).isEqualTo(expectedResult)
            }
        }

    @Test
    fun `test that typed node is downloaded to cache`() = runTest {
        mockStatic(FileUtil::class.java).use {
            val nodeHandle = 343L
            val nodeExtension = "txt"
            val nodeName = "text.$nodeExtension"

            val testIntent = mock<Intent> {
                on { getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) } doReturn nodeHandle
            }
            val typedNode = mock<TypedFileNode>()
            val destination = "/cache/folder/$nodeName"

            val megaNode = mock<MegaNode> {
                on { handle } doReturn nodeHandle
                on { name } doReturn nodeName
            }
            val file = File(destination)

            whenever(megaApi.getNodeByHandle(nodeHandle)) doReturn megaNode
            whenever(getNodeByIdUseCase(NodeId(nodeHandle))) doReturn typedNode
            whenever(FileUtil.getLocalFile(megaNode)) doReturn null
            whenever(
                getCacheFileUseCase(eq(CacheFolderConstant.TEMPORARY_FOLDER), anyString())
            ) doReturn file
            whenever(downloadNodeUseCase(any(), anyString(), any(), any())) doReturn emptyFlow()

            underTest.setInitialValues(testIntent, mock())
            underTest.readFileContent()

            advanceUntilIdle()

            verify(downloadNodeUseCase)(
                node = typedNode,
                destinationPath = destination,
                appData = listOf(TransferAppData.BackgroundTransfer),
                isHighPriority = true,
            )
        }
    }


    private fun provideIsMarkDownFileTestData() = listOf(
        arrayOf("file.md", true),
        arrayOf("file.txt", false),
        arrayOf(null, false),
    )

    @Test
    fun `test that getNode returns correct node for Edit mode`() = runTest {
        // Given
        val testIntent = createTestIntent(handle = 456L)
        underTest.setInitialValues(testIntent, mock())

        val mockFileNode = createMockFileNode()
        val mockTypedNode = createMockTypedNode()

        whenever(getNodeByIdUseCase(NodeId(456L))).thenReturn(mockTypedNode)
        whenever(megaApi.getNodeByHandle(456L)).thenReturn(mockFileNode)
        setupTextEditorData(mockFileNode)

        // When
        val node = underTest.getNode()

        // Then
        assertThat(node).isNotNull()
        assertThat(node?.handle).isEqualTo(456L)
        assertThat(node?.parentHandle).isEqualTo(123L)
        assertThat(node?.name).isEqualTo("test.txt")
    }

    @Test
    fun `test that getNodeAccess returns correct access level`() = runTest {
        // Given
        val testIntent = createTestIntent(handle = 456L)
        underTest.setInitialValues(testIntent, mock())

        val mockFileNode = createMockFileNode()
        val mockTypedNode = createMockTypedNode()

        whenever(getNodeByIdUseCase(NodeId(456L))).thenReturn(mockTypedNode)
        whenever(megaApi.getNodeByHandle(456L)).thenReturn(mockFileNode)
        whenever(megaApi.getAccess(mockFileNode)).thenReturn(1) // Full access
        setupTextEditorData(mockFileNode)

        // When
        val accessLevel = underTest.getNodeAccess()

        // Then
        assertThat(accessLevel).isEqualTo(1)
    }

    @Test
    fun `test that getNodeAccess returns correct access level for different permissions`() =
        runTest {
            // Given
            val testIntent = createTestIntent(handle = 789L)
            underTest.setInitialValues(testIntent, mock())

            val mockFileNode = createMockFileNode(handle = 789L, parentHandle = 456L)
            val mockTypedNode = createMockTypedNode(789L)

            whenever(getNodeByIdUseCase(NodeId(789L))).thenReturn(mockTypedNode)
            whenever(megaApi.getNodeByHandle(789L)).thenReturn(mockFileNode)
            whenever(megaApi.getAccess(mockFileNode)).thenReturn(0) // Read-only access
            setupTextEditorData(mockFileNode)

            // When
            val accessLevel = underTest.getNodeAccess()

            // Then
            assertThat(accessLevel).isEqualTo(0)
        }

    @Test
    fun `test that isFileEdited returns true when text has been modified`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())
        setupPagination("Original content")

        // When
        underTest.setEditedText("Modified content")

        // Then
        assertThat(underTest.isFileEdited()).isTrue()
    }

    @Test
    fun `test that isFileEdited returns false when text has not been modified`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())
        setupPagination("Original content")

        // When - don't modify the text

        // Then
        assertThat(underTest.isFileEdited()).isFalse()
    }

    @Test
    fun `test that isFileEdited returns false when pagination is null`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())
        // Don't set up pagination - leave it null

        // When & Then
        assertThat(underTest.isFileEdited()).isFalse()
    }

    @Test
    fun `test that saveFile handles errors gracefully`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())

        // Mock API to return null for rootNode property (error case)
        doReturn(null).whenever(megaApi).rootNode

        // When
        underTest.saveFile(fromHome = false)

        // Then
        // Verify that no transfer event is triggered due to early return
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.transferEvent).isNotInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that setEditMode correctly sets Edit mode`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())

        // When
        underTest.setEditMode()

        // Then
        assertThat(underTest.isEditMode()).isTrue()
        assertThat(underTest.isViewMode()).isFalse()
        assertThat(underTest.isCreateMode()).isFalse()
    }

    @Test
    fun `test that setEditedText updates pagination correctly`() = runTest {
        // Given
        val testIntent = createTestIntent()
        underTest.setInitialValues(testIntent, mock())
        setupPagination("Original content")

        // When
        val newText = "Updated content with changes"
        underTest.setEditedText(newText)

        // Then
        val currentText = underTest.getCurrentText()
        assertThat(currentText).isEqualTo(newText)
        assertThat(underTest.isFileEdited()).isTrue()
    }

    // Helper methods to reduce duplication and improve maintainability
    private fun createTestIntent(
        fileName: String = "test.txt",
        mode: TextEditorMode = TextEditorMode.Edit,
        handle: Long? = null,
    ): Intent = Intent().apply {
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, fileName)
        putExtra(MODE, mode.value)
        handle?.let { putExtra(INTENT_EXTRA_KEY_HANDLE, it) }
    }

    private fun createMockFileNode(
        handle: Long = 456L,
        parentHandle: Long = 123L,
        name: String = "test.txt",
    ): MegaNode = mock<MegaNode>().apply {
        whenever(this.handle).thenReturn(handle)
        whenever(this.parentHandle).thenReturn(parentHandle)
        whenever(this.name).thenReturn(name)
    }

    private fun createMockTypedNode(nodeId: Long = 456L): TypedNode = mock<TypedNode>().apply {
        whenever(this.id).thenReturn(NodeId(nodeId))
    }

    private fun setupTextEditorData(node: MegaNode) {
        val textEditorData = TextEditorData(node = node)
        val textEditorDataField = TextEditorViewModel::class.java.getDeclaredField("textEditorData")
        textEditorDataField.isAccessible = true
        val textEditorDataLiveData =
            textEditorDataField.get(underTest) as MutableLiveData<TextEditorData>
        textEditorDataLiveData.value = textEditorData
    }

    private fun setupPagination(content: String = "Original content") {
        val pagination = Pagination(content)
        val paginationField = TextEditorViewModel::class.java.getDeclaredField("pagination")
        paginationField.isAccessible = true
        val paginationLiveData = paginationField.get(underTest) as MutableLiveData<Pagination>
        paginationLiveData.value = pagination
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}