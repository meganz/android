package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.SendToChatResult
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.ExportNodeUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel.Args
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeEffect
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorComposeViewModelTest {

    private val getTextContentForTextEditorUseCase: GetTextContentForTextEditorUseCase = mock()
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getNodeAccessUseCase: GetNodeAccessUseCase = mock()
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase = mock()
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase = mock()
    private val exportNodeUseCase: ExportNodeUseCase = mock()
    private val textEditorBottomBarActionsMapper: TextEditorBottomBarActionsMapper =
        TextEditorBottomBarActionsMapper()

    private lateinit var underTest: TextEditorComposeViewModel

    @BeforeEach
    fun resetMocks() {
        reset(
            getTextContentForTextEditorUseCase,
            saveTextContentForTextEditorUseCase,
            getNodeByIdUseCase,
            getNodeAccessUseCase,
            attachMultipleNodesUseCase,
            get1On1ChatIdUseCase,
            exportNodeUseCase,
        )
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
    }

    private fun initUnderTest(
        nodeHandle: Long = 0L,
        mode: TextEditorMode = TextEditorMode.View,
        fileName: String? = null,
        inExcludedAdapterForGetLinkAndEdit: Boolean = false,
        showDownload: Boolean = true,
        showShare: Boolean = true,
        showSendToChat: Boolean = false,
        isFromSharedFolder: Boolean = false,
        fromHome: Boolean = false,
        localPath: String? = null,
        defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    ) {
        underTest = TextEditorComposeViewModel(
            args = Args(
                nodeHandle = nodeHandle,
                mode = mode,
                fileName = fileName,
                inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit,
                showDownload = showDownload,
                showShare = showShare,
                showSendToChat = showSendToChat,
                isFromSharedFolder = isFromSharedFolder,
                fromHome = fromHome,
                localPath = localPath,
            ),
            defaultDispatcher = defaultDispatcher,
            getTextContentForTextEditorUseCase = getTextContentForTextEditorUseCase,
            saveTextContentForTextEditorUseCase = saveTextContentForTextEditorUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase,
            textEditorBottomBarActionsMapper = textEditorBottomBarActionsMapper,
            attachMultipleNodesUseCase = attachMultipleNodesUseCase,
            get1On1ChatIdUseCase = get1On1ChatIdUseCase,
            exportNodeUseCase = exportNodeUseCase,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that initial uiState reflects Args`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "notes.txt",
        )
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.fileName).isEqualTo("notes.txt")
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `test that uiState with Edit mode reflects Args`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.Edit,
            fileName = "notes.txt",
        )
        val state = underTest.uiState.value
        assertThat(state.fileName).isEqualTo("notes.txt")
        assertThat(state.mode).isEqualTo(TextEditorMode.Edit)
    }

    @Test
    fun `test that Args with View mode sets mode to View`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "readme.txt",
        )
        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.View)
    }

    @Test
    fun `test that Args with Create mode sets mode to Create`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.Create)
    }

    @Test
    fun `test that null fileName results in empty string in uiState`() {
        initUnderTest(fileName = null)
        assertThat(underTest.uiState.value.fileName).isEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that fileName is set from node when opening file in View mode`() = runTest {
        val node = mock<TypedNode>()
        whenever(node.name).thenReturn("fetched-name.txt")
        whenever(node.exportedData).thenReturn(null)
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
        }
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "caller-given.txt",
        )
        advanceUntilIdle()
        assertThat(underTest.uiState.value.fileName).isEqualTo("fetched-name.txt")
    }

    @Test
    fun `test that Create mode keeps fileName from Args`() {
        initUnderTest(mode = TextEditorMode.Create, fileName = "newfile.txt")
        assertThat(underTest.uiState.value.fileName).isEqualTo("newfile.txt")
    }

    @Test
    fun `test that Create mode is not loading`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.uiState.value.isLoading).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onMenuAction Download emits StartDownloadNode transferEvent when node exists`() = runTest {
        val node = mock<TypedNode>()
        runBlocking { whenever(getNodeByIdUseCase(NodeId(5L))).thenReturn(node) }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Download)
        advanceUntilIdle()

        val event = underTest.uiState.value.transferEvent
        check(event is StateEventWithContentTriggered<*>)
        val content = event.content
        assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes).containsExactly(node)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onMenuAction Download does not emit transferEvent when node is null`() = runTest {
        runBlocking { whenever(getNodeByIdUseCase(any())).thenReturn(null) }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onMenuAction(TextEditorTopBarAction.Download)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onMenuAction LineNumbers toggles showLineNumbers`() {
        initUnderTest()
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        assertThat(underTest.uiState.value.showLineNumbers).isTrue()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()
    }

    @Test
    fun `test that onMenuAction SendToChat triggers SendToChat node effect`() {
        initUnderTest(nodeHandle = 12L)
        underTest.onMenuAction(TextEditorTopBarAction.SendToChat)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.SendToChat(12L))
    }


    @Test
    fun `test that onMenuAction GetLink triggers ManageLink node effect`() {
        initUnderTest(nodeHandle = 9L)
        underTest.onMenuAction(TextEditorTopBarAction.GetLink)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.ManageLink(9L))
    }

    @Test
    fun `test that onMenuAction Share triggers Share node effect`() {
        initUnderTest(nodeHandle = 3L, fileName = "doc.txt")
        underTest.onMenuAction(TextEditorTopBarAction.Share)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(
            TextEditorNodeEffect.Share(
                nodeHandle = 3L,
                localPath = null,
                fileName = "doc.txt",
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns true when chunk state is edited`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "modified content") }

        assertThat(underTest.isContentDirty()).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns false when no chunks are edited`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.getOrCreateChunkState(0)
        assertThat(underTest.isContentDirty()).isFalse()
    }

    @Test
    fun `test that isContentDirty returns false in Create mode with no edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)
        assertThat(underTest.isContentDirty()).isFalse()
    }

    @Test
    fun `test that confirmDiscard in Create mode closes dialog and emits exitAfterCreateDiscardEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.requestShowDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()

        underTest.confirmDiscard()

        val state = underTest.uiState.value
        assertThat(state.showDiscardDialog).isFalse()
        assertThat(state.exitAfterCreateDiscardEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that consumeExitAfterCreateDiscardEvent resets exitAfterCreateDiscardEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.confirmDiscard()
        assertThat(underTest.uiState.value.exitAfterCreateDiscardEvent).isEqualTo(triggered)

        underTest.consumeExitAfterCreateDiscardEvent()
        assertThat(underTest.uiState.value.exitAfterCreateDiscardEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile forwards fromHome from Args to save use case`() = runTest {
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/new.txt",
            parentHandle = 1L,
            isEditMode = false,
            fromHome = true,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(mode = TextEditorMode.Create, fromHome = true)
        advanceUntilIdle()

        underTest.saveFile()
        advanceUntilIdle()

        val fromHomeCaptor = argumentCaptor<Boolean>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(),
            any(),
            any(),
            any(),
            fromHomeCaptor.capture(),
            any(),
        )
        assertThat(fromHomeCaptor.firstValue).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that confirmDiscard in Create mode does not invoke save use case`() = runTest {
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(
                TextEditorSaveResult.UploadRequired(
                    tempPath = "/tmp/x.txt",
                    parentHandle = 1L,
                    isEditMode = false,
                    fromHome = false,
                ),
            )
        initUnderTest(mode = TextEditorMode.Create)
        advanceUntilIdle()
        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }

        underTest.confirmDiscard()
        advanceUntilIdle()

        verify(saveTextContentForTextEditorUseCase, never()).invoke(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile in Create mode emits exitAfterCreateSaveEvent and not saveSuccessEvent`() =
        runTest {
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    TextEditorSaveResult.UploadRequired(
                        tempPath = "/tmp/new.txt",
                        parentHandle = 1L,
                        isEditMode = false,
                        fromHome = false,
                    )
                )
            initUnderTest(mode = TextEditorMode.Create)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.exitAfterCreateSaveEvent).isEqualTo(triggered)
            assertThat(state.saveSuccessEvent).isEqualTo(consumed)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile in Edit mode emits saveSuccessEvent and not exitAfterCreateSaveEvent`() =
        runTest {
            val lines = listOf("hello")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(
                    TextEditorSaveResult.UploadRequired(
                        tempPath = "/tmp/edit.txt",
                        parentHandle = 1L,
                        isEditMode = true,
                        fromHome = false,
                    )
                )
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.saveSuccessEvent).isEqualTo(triggered)
            assertThat(state.exitAfterCreateSaveEvent).isEqualTo(consumed)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns true after chunk disposed with edits`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }
        underTest.disposeChunkState(0)

        assertThat(underTest.isContentDirty()).isTrue()
    }

    @Test
    fun `test that shouldPopDestinationOnCleanEditExit is true when opened in Edit mode`() {
        initUnderTest(mode = TextEditorMode.Edit)
        assertThat(underTest.shouldPopDestinationOnCleanEditExit()).isTrue()
    }

    @Test
    fun `test that shouldPopDestinationOnCleanEditExit is false when opened in View mode`() {
        initUnderTest(mode = TextEditorMode.View)
        assertThat(underTest.shouldPopDestinationOnCleanEditExit()).isFalse()
    }

    @Test
    fun `test that consumeErrorEvent consumes errorEvent`() {
        initUnderTest()
        underTest.consumeErrorEvent()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getText failure triggers errorEvent and clears loading`() = runTest {
        doReturn(flow<List<String>> { throw RuntimeException("load failed") })
            .whenever(getTextContentForTextEditorUseCase).invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(triggered)
        assertThat(underTest.uiState.value.isLoading).isFalse()
        assertThat(underTest.uiState.value.errorMessage).isEqualTo("load failed")
    }

    @Test
    fun `test that setEditMode updates uiState to edit mode`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "a.txt",
        )
        underTest.setEditMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.Edit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that setEditMode creates chunks from loaded content`() = runTest {
        val allLines = (1..1500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.setEditMode()

        assertThat(underTest.getChunkCount()).isEqualTo(8)
        val chunk0State = underTest.getOrCreateChunkState(0)
        val chunk0Text = chunk0State.text.toString()
        assertThat(chunk0Text.split("\n").first()).isEqualTo("line1")
        assertThat(chunk0Text.split("\n")).hasSize(CHUNK_SIZE)
    }

    @Test
    fun `test that setViewMode updates uiState to view mode`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.Edit,
            fileName = "a.txt",
        )
        underTest.setViewMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that setViewMode without edits switches directly to View`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.setViewMode()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isRestoringContent).isFalse()
    }

    @Test
    fun `test that onBottomBarAction Edit sets mode to Edit`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "a.txt",
        )
        underTest.onBottomBarAction(TextEditorBottomBarAction.Edit)

        assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.Edit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction Download emits StartDownloadNode transferEvent when node exists`() = runTest {
        val node = mock<TypedNode>()
        runBlocking { whenever(getNodeByIdUseCase(NodeId(42L))).thenReturn(node) }
        initUnderTest(nodeHandle = 42L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.onBottomBarAction(TextEditorBottomBarAction.Download)
        advanceUntilIdle()

        val event = underTest.uiState.value.transferEvent
        check(event is StateEventWithContentTriggered<*>)
        val content = event.content
        assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
        assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes).containsExactly(node)
    }

    @Test
    fun `test that onBottomBarAction GetLink triggers ManageLink node effect`() {
        initUnderTest(nodeHandle = 99L, mode = TextEditorMode.View)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.ManageLink(99L))
    }

    @Test
    fun `test that onBottomBarAction Share triggers Share node effect`() {
        initUnderTest(nodeHandle = 7L, mode = TextEditorMode.View, fileName = "a.txt")
        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(
            TextEditorNodeEffect.Share(
                nodeHandle = 7L,
                localPath = null,
                fileName = "a.txt",
            ),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction SendToChat triggers SendToChat node effect`() = runTest {
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        underTest.onBottomBarAction(TextEditorBottomBarAction.SendToChat)
        val ev = underTest.uiState.value.nodeEffectEvent
        check(ev is StateEventWithContentTriggered<*>)
        assertThat(ev.content).isEqualTo(TextEditorNodeEffect.SendToChat(1L))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that View mode loads bottomBarActions from node and access and updates uiState`() = runTest {
        val node = mock<TypedNode>()
        whenever(node.exportedData).thenReturn(mock())
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(node)
            whenever(getNodeAccessUseCase(any())).thenReturn(AccessPermission.OWNER)
        }
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View, showShare = false)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.bottomBarActions)
            .containsExactly(TextEditorBottomBarAction.Download, TextEditorBottomBarAction.Edit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that gradual load updates totalLineCount`() = runTest {
        val chunk1 = (1..500).map { "line$it" }
        val chunk2 = (501..1000).map { "line$it" }
        doReturn(flowOf(chunk1, chunk2)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.totalLineCount).isEqualTo(1000)
        assertThat(state.isFullyLoaded).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkText returns correct lines for chunk index`() = runTest {
        val allLines = (1..200).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val chunk0 = underTest.getChunkText(0)
        val chunk0Lines = chunk0.split("\n")
        assertThat(chunk0Lines).hasSize(CHUNK_SIZE)
        assertThat(chunk0Lines.first()).isEqualTo("line1")
        assertThat(chunk0Lines.last()).isEqualTo("line${CHUNK_SIZE}")

        val lastChunkIndex = (200 - 1) / CHUNK_SIZE
        val lastChunk = underTest.getChunkText(lastChunkIndex)
        val lastChunkLines = lastChunk.split("\n")
        assertThat(lastChunkLines.last()).isEqualTo("line200")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkText returns empty for out of range index`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkText(999)).isEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that disposeChunkState flushes edits back to chunk data`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "EDITED\nline2") }
        underTest.disposeChunkState(0)

        val newState = underTest.getOrCreateChunkState(0)
        assertThat(newState.text.toString()).startsWith("EDITED")
    }

    @Test
    fun `test that requestShowDiscardDialog sets showDiscardDialog to true`() {
        initUnderTest(mode = TextEditorMode.Edit)
        underTest.requestShowDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
    }

    @Test
    fun `test that dismissDiscardDialog sets showDiscardDialog to false`() {
        initUnderTest(mode = TextEditorMode.Edit)
        underTest.requestShowDiscardDialog()
        underTest.dismissDiscardDialog()
        assertThat(underTest.uiState.value.showDiscardDialog).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that confirmDiscard restores content and switches to View mode`() = runTest {
        val lines = listOf("line1", "line2", "line3")
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.confirmDiscard()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isRestoringContent).isFalse()
        assertThat(state.showDiscardDialog).isFalse()
        assertThat(underTest.getChunkText(0)).isEqualTo(lines.joinToString("\n"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that setViewMode with discardChanges sets isRestoringContent to true during restore`() =
        runTest {
            val lines = listOf("line1", "line2")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(
                nodeHandle = 1L,
                mode = TextEditorMode.Edit,
                defaultDispatcher = StandardTestDispatcher(testScheduler),
            )
            advanceUntilIdle()

            underTest.requestShowDiscardDialog()
            assertThat(underTest.uiState.value.showDiscardDialog).isTrue()

            underTest.setViewMode(discardChanges = true)
            assertThat(underTest.uiState.value.isRestoringContent).isTrue()

            advanceUntilIdle()
            assertThat(underTest.uiState.value.isRestoringContent).isFalse()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile flushes edits and saves full content`() = runTest {
        val allLines = (1..100).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/test.txt",
            parentHandle = 1L,
            isEditMode = true,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, 5, "EDITED") }
        underTest.saveFile()
        advanceUntilIdle()

        val textCaptor = argumentCaptor<String>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(), textCaptor.capture(), any(), any(), any(), any(),
        )
        val savedLines = textCaptor.firstValue.split("\n")
        assertThat(savedLines.first()).startsWith("EDITED")
        assertThat(savedLines).hasSize(100)
        assertThat(savedLines.last()).isEqualTo("line100")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile in Create mode saves content as-is`() = runTest {
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/new.txt",
            parentHandle = 1L,
            isEditMode = false,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Create)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "brand new content") }
        underTest.saveFile()
        advanceUntilIdle()

        val textCaptor = argumentCaptor<String>()
        verify(saveTextContentForTextEditorUseCase).invoke(
            any(), textCaptor.capture(), any(), any(), any(), any(),
        )
        assertThat(textCaptor.firstValue).isEqualTo("brand new content")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile saves when started in View mode and switched to Edit mode`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/edited.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()
            underTest.setEditMode()
            underTest.saveFile()
            advanceUntilIdle()

            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), any(), any(), any(), any(),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile sets saveSuccessEvent to triggered and mode to View on success`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/test.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.saveSuccessEvent).isEqualTo(triggered)
            assertThat(state.mode).isEqualTo(TextEditorMode.View)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that consumeSaveSuccessEvent resets the event`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/test.txt",
                parentHandle = 1L,
                isEditMode = true,
                fromHome = false,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)

            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.saveFile()
            advanceUntilIdle()
            assertThat(underTest.uiState.value.saveSuccessEvent).isEqualTo(triggered)

            underTest.consumeSaveSuccessEvent()
            assertThat(underTest.uiState.value.saveSuccessEvent).isEqualTo(consumed)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile failure triggers errorEvent and sets errorMessage`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("disk full"))

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.saveFile()
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.errorEvent).isEqualTo(triggered)
        assertThat(state.errorMessage).isEqualTo("disk full")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile is no-op when mode is View`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        val before = underTest.uiState.value
        underTest.saveFile()
        advanceUntilIdle()
        assertThat(underTest.uiState.value).isEqualTo(before)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkStartLine returns 1 for first chunk`() = runTest {
        val allLines = (1..400).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkStartLine(0)).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkStartLine returns correct offset for second chunk`() = runTest {
        val allLines = (1..400).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkStartLine(1)).isEqualTo(CHUNK_SIZE + 1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that setFocusedEditChunk updates focusedEditChunk in uiState`() = runTest {
        val allLines = (1..400).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.setFocusedEditChunk(1)
        assertThat(underTest.uiState.value.focusedEditChunk).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that consumeTransferEvent resets transfer event`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        val saveResult = TextEditorSaveResult.UploadRequired(
            tempPath = "/tmp/test.txt",
            parentHandle = 1L,
            isEditMode = true,
            fromHome = false,
        )
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenReturn(saveResult)

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()
        underTest.saveFile()
        advanceUntilIdle()

        underTest.consumeTransferEvent()
        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that consumeNodeEffectEvent resets node effect`() {
        initUnderTest(nodeHandle = 2L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        check(underTest.uiState.value.nodeEffectEvent is StateEventWithContentTriggered<*>)
        underTest.consumeNodeEffectEvent()
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction Share emits resolvedPublicLink when exportNodeUseCase returns a link`() =
        runTest {
            val publicLink = "https://mega.nz/file/abc123"
            runBlocking {
                whenever(getNodeByIdUseCase(NodeId(5L))).thenReturn(null)
                whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenReturn(publicLink)
            }
            initUnderTest(nodeHandle = 5L, fileName = "doc.txt")
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            val ev = underTest.uiState.value.nodeEffectEvent
            check(ev is StateEventWithContentTriggered<*>)
            assertThat(ev.content).isEqualTo(
                TextEditorNodeEffect.Share(
                    nodeHandle = 5L,
                    localPath = null,
                    fileName = "doc.txt",
                    resolvedPublicLink = publicLink,
                ),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction Share uses existing public link from node when already exported`() =
        runTest {
            val existingLink = "https://mega.nz/file/existing"
            val node = mock<TypedNode>()
            whenever(node.exportedData).thenReturn(ExportedData(publicLink = existingLink, publicLinkCreationTime = 0L))
            runBlocking { whenever(getNodeByIdUseCase(NodeId(7L))).thenReturn(node) }
            initUnderTest(nodeHandle = 7L, fileName = "report.pdf")
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            val ev = underTest.uiState.value.nodeEffectEvent
            check(ev is StateEventWithContentTriggered<*>)
            assertThat(ev.content).isEqualTo(
                TextEditorNodeEffect.Share(
                    nodeHandle = 7L,
                    localPath = null,
                    fileName = "report.pdf",
                    resolvedPublicLink = existingLink,
                ),
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction Share triggers shareErrorEvent when exportNodeUseCase throws`() =
        runTest {
            runBlocking {
                whenever(getNodeByIdUseCase(any())).thenReturn(null)
                whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenThrow(RuntimeException("export failed"))
            }
            initUnderTest(nodeHandle = 5L)
            advanceUntilIdle()

            underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(triggered)
        }

    @Test
    fun `test that GetLink and SendToChat are no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
        underTest.onBottomBarAction(TextEditorBottomBarAction.SendToChat)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onBottomBarAction Download is no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Download)
        assertThat(underTest.uiState.value.transferEvent).isEqualTo(consumed())
    }

    @Test
    fun `test that onBottomBarAction Share is no-op when node handle is invalid`() {
        initUnderTest(nodeHandle = -1L)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        assertThat(underTest.uiState.value.nodeEffectEvent).isEqualTo(consumed())
        assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that consumeShareErrorEvent resets shareErrorEvent`() = runTest {
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(exportNodeUseCase(any(), anyOrNull(), any())).thenThrow(RuntimeException())
        }
        initUnderTest(nodeHandle = 5L)
        advanceUntilIdle()

        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)
        advanceUntilIdle()
        check(underTest.uiState.value.shareErrorEvent == triggered)

        underTest.consumeShareErrorEvent()
        assertThat(underTest.uiState.value.shareErrorEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that attachNodesToChat passes combined chatIds from user handles and direct chatIds`() =
        runTest {
            val chatIdFromHandle1 = 100L
            val chatIdFromHandle2 = 200L
            whenever(get1On1ChatIdUseCase(10L)).thenReturn(chatIdFromHandle1)
            whenever(get1On1ChatIdUseCase(20L)).thenReturn(chatIdFromHandle2)
            initUnderTest(nodeHandle = 5L)

            val result = SendToChatResult(
                nodeIds = longArrayOf(5L),
                chatIds = longArrayOf(300L),
                userHandles = longArrayOf(10L, 20L),
            )
            underTest.attachNodesToChat(result)
            advanceUntilIdle()

            val chatIdsCaptor = argumentCaptor<List<Long>>()
            verify(attachMultipleNodesUseCase).invoke(any(), chatIdsCaptor.capture())
            assertThat(chatIdsCaptor.firstValue).containsExactly(100L, 200L, 300L)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that attachNodesToChat skips user handles that fail to resolve`() = runTest {
        whenever(get1On1ChatIdUseCase(10L)).thenReturn(100L)
        whenever(get1On1ChatIdUseCase(20L)).thenThrow(RuntimeException("resolve failed"))
        initUnderTest(nodeHandle = 5L)

        val result = SendToChatResult(
            nodeIds = longArrayOf(5L),
            chatIds = longArrayOf(),
            userHandles = longArrayOf(10L, 20L),
        )
        underTest.attachNodesToChat(result)
        advanceUntilIdle()

        val chatIdsCaptor = argumentCaptor<List<Long>>()
        verify(attachMultipleNodesUseCase).invoke(any(), chatIdsCaptor.capture())
        assertThat(chatIdsCaptor.firstValue).containsExactly(100L)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that attachNodesToChat emits sendToChatErrorEvent when attachMultipleNodesUseCase throws`() =
        runTest {
            whenever(get1On1ChatIdUseCase(any())).thenReturn(100L)
            whenever(attachMultipleNodesUseCase(any(), any()))
                .thenThrow(RuntimeException("attach failed"))
            initUnderTest(nodeHandle = 5L)

            val result = SendToChatResult(
                nodeIds = longArrayOf(5L),
                chatIds = longArrayOf(),
                userHandles = longArrayOf(10L),
            )
            underTest.attachNodesToChat(result)
            advanceUntilIdle()

            assertThat(underTest.uiState.value.sendToChatErrorEvent).isEqualTo(triggered)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that consumeSendToChatErrorEvent resets sendToChatErrorEvent`() = runTest {
        whenever(get1On1ChatIdUseCase(any())).thenReturn(100L)
        whenever(attachMultipleNodesUseCase(any(), any()))
            .thenThrow(RuntimeException("attach failed"))
        initUnderTest(nodeHandle = 5L)

        val result = SendToChatResult(
            nodeIds = longArrayOf(5L),
            chatIds = longArrayOf(),
            userHandles = longArrayOf(10L),
        )
        underTest.attachNodesToChat(result)
        advanceUntilIdle()
        check(underTest.uiState.value.sendToChatErrorEvent == triggered)

        underTest.consumeSendToChatErrorEvent()
        assertThat(underTest.uiState.value.sendToChatErrorEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkCount returns correct count in View mode`() = runTest {
        val allLines = (1..500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.getChunkCount()).isEqualTo(3)
    }

    @Test
    fun `test that getChunkCount returns 1 for Create mode with empty content`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.getChunkCount()).isEqualTo(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that getChunkCount returns correct count in Edit mode`() = runTest {
        val allLines = (1..500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        underTest.setEditMode()
        assertThat(underTest.getChunkCount()).isEqualTo(3)
    }

    @Test
    fun `test that handleClose emits closeEvent when Create mode has no edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)

        underTest.handleClose()

        assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)
    }

    @Test
    fun `test that handleClose shows discard dialog when Create mode has edits`() {
        initUnderTest(mode = TextEditorMode.Create)
        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "edited") }

        underTest.handleClose()

        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handleClose emits closeEvent when Edit mode opened as Edit has no edits`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.handleClose()

            assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handleClose switches to View mode when Edit mode opened as View has no edits`() =
        runTest {
            val lines = listOf("line1")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            underTest.setEditMode()
            underTest.handleClose()

            assertThat(underTest.uiState.value.mode).isEqualTo(TextEditorMode.View)
            assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that handleClose shows discard dialog when Edit mode has edits`() = runTest {
        val lines = (1..100).map { "line$it" }
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        val chunkState = underTest.getOrCreateChunkState(0)
        chunkState.edit { replace(0, length, "modified") }

        underTest.handleClose()

        assertThat(underTest.uiState.value.showDiscardDialog).isTrue()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }

    @Test
    fun `test that consumeCloseEvent resets closeEvent`() {
        initUnderTest(mode = TextEditorMode.Create)
        underTest.getOrCreateChunkState(0)
        underTest.handleClose()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(triggered)

        underTest.consumeCloseEvent()
        assertThat(underTest.uiState.value.closeEvent).isEqualTo(consumed)
    }
}
