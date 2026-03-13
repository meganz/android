package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel.Args
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorBottomBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorNodeActionRequest
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.navigation.contract.TransferHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
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
    private val nodeActionHandler: TextEditorNodeActionHandler = mock()
    private val transferHandler: TransferHandler = mock()

    private lateinit var underTest: TextEditorComposeViewModel

    @BeforeEach
    fun resetMocks() {
        reset(getTextContentForTextEditorUseCase, saveTextContentForTextEditorUseCase, getNodeByIdUseCase, getNodeAccessUseCase, nodeActionHandler, transferHandler)
        runBlocking {
            whenever(getNodeByIdUseCase(any())).thenReturn(null)
            whenever(getNodeAccessUseCase(any())).thenReturn(null)
        }
    }

    private fun initUnderTest(
        nodeHandle: Long = 0L,
        mode: TextEditorMode = TextEditorMode.View,
        nodeSourceType: Int? = null,
        fileName: String? = null,
        inExcludedAdapterForGetLinkAndEdit: Boolean = false,
        showDownload: Boolean = true,
        showShare: Boolean = true,
        transferHandler: TransferHandler = this.transferHandler,
        localPath: String? = null,
        defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    ) {
        underTest = TextEditorComposeViewModel(
            args = Args(
                nodeHandle = nodeHandle,
                mode = mode,
                nodeSourceType = nodeSourceType,
                fileName = fileName,
                inExcludedAdapterForGetLinkAndEdit = inExcludedAdapterForGetLinkAndEdit,
                showDownload = showDownload,
                showShare = showShare,
                transferHandler = transferHandler,
                localPath = localPath,
            ),
            defaultDispatcher = defaultDispatcher,
            getTextContentForTextEditorUseCase = getTextContentForTextEditorUseCase,
            saveTextContentForTextEditorUseCase = saveTextContentForTextEditorUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodeAccessUseCase = getNodeAccessUseCase,
            nodeActionHandler = nodeActionHandler,
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

    @Test
    fun `test that onMenuAction Download does not crash`() {
        initUnderTest()
        underTest.onMenuAction(TextEditorTopBarAction.Download)
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()
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
    fun `test that onMenuAction SendToChat does not change state`() {
        initUnderTest()
        val before = underTest.uiState.value
        underTest.onMenuAction(TextEditorTopBarAction.SendToChat)
        val after = underTest.uiState.value
        assertThat(after).isEqualTo(before)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onMenuAction Download GetLink Share do not change state`() = runTest {
        initUnderTest()
        advanceUntilIdle()
        val before = underTest.uiState.value
        underTest.onMenuAction(TextEditorTopBarAction.Download)
        assertThat(underTest.uiState.value).isEqualTo(before)
        underTest.onMenuAction(TextEditorTopBarAction.GetLink)
        assertThat(underTest.uiState.value).isEqualTo(before)
        underTest.onMenuAction(TextEditorTopBarAction.Share)
        assertThat(underTest.uiState.value).isEqualTo(before)
    }

    @Test
    fun `test that Create mode with null params has content empty and not loading`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.uiState.value.content).isEmpty()
        assertThat(underTest.uiState.value.isLoading).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns true when text differs from loaded content`() = runTest {
        val lines = listOf("line1", "line2", "line3")
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        assertThat(underTest.isContentDirty("modified content")).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns false when text matches loaded content`() = runTest {
        val lines = listOf("line1", "line2", "line3")
        doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        assertThat(underTest.isContentDirty(lines.joinToString("\n"))).isFalse()
    }

    @Test
    fun `test that isContentDirty returns true for non-empty text in Create mode`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.isContentDirty("hello")).isTrue()
    }

    @Test
    fun `test that isContentDirty returns false for empty text in Create mode`() {
        initUnderTest(mode = TextEditorMode.Create)
        assertThat(underTest.isContentDirty("")).isFalse()
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
        assertThat(underTest.uiState.value.loadErrorMessage).isEqualTo("load failed")
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

    @Test
    fun `test that onBottomBarAction Download calls nodeActionHandler with Download request`() {
        initUnderTest(nodeHandle = 42L, mode = TextEditorMode.View)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Download)

        verify(nodeActionHandler).handle(
            any(),
            eq(TextEditorNodeActionRequest.Download(42L)),
            eq(transferHandler),
        )
    }

    @Test
    fun `test that onBottomBarAction GetLink calls nodeActionHandler with GetLink request`() {
        initUnderTest(nodeHandle = 99L, mode = TextEditorMode.View)
        underTest.onBottomBarAction(TextEditorBottomBarAction.GetLink)

        verify(nodeActionHandler).handle(
            any(),
            eq(TextEditorNodeActionRequest.GetLink(99L)),
            eq(transferHandler),
        )
    }

    @Test
    fun `test that onBottomBarAction Share calls nodeActionHandler with Share request`() {
        initUnderTest(nodeHandle = 7L, mode = TextEditorMode.View)
        underTest.onBottomBarAction(TextEditorBottomBarAction.Share)

        verify(nodeActionHandler).handle(
            any(),
            eq(TextEditorNodeActionRequest.Share(7L)),
            eq(transferHandler),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onBottomBarAction SendToChat does not change state`() = runTest {
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        val before = underTest.uiState.value
        underTest.onBottomBarAction(TextEditorBottomBarAction.SendToChat)
        val after = underTest.uiState.value
        assertThat(after).isEqualTo(before)
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
    fun `test that gradual load emits chunks and updates content hasMoreLines and totalLinesLoaded`() =
        runTest {
            val chunk1 = (1..500).map { "line$it" }
            val chunk2 = (501..1000).map { "line$it" }
            val chunk3 = (1001..1500).map { "line$it" }
            doReturn(flowOf(chunk1, chunk2, chunk3)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.totalLinesLoaded).isEqualTo(1500)
            assertThat(state.hasMoreLines).isTrue()
            assertThat(state.isFullyLoaded).isTrue()
            val displayedLines = state.content.split("\n")
            assertThat(displayedLines).hasSize(1000)
            assertThat(displayedLines.first()).isEqualTo("line1")
            assertThat(displayedLines.last()).isEqualTo("line1000")
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onLoadMoreLines increases displayed content when hasMoreLines is true`() =
        runTest {
            val chunk1 = (1..500).map { "line$it" }
            val chunk2 = (501..1500).map { "line$it" }
            doReturn(flowOf(chunk1, chunk2)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
            advanceUntilIdle()

            var displayedLines = underTest.uiState.value.content.split("\n")
            assertThat(underTest.uiState.value.hasMoreLines).isTrue()
            assertThat(displayedLines).hasSize(1000)

            underTest.onLoadMoreLines()

            displayedLines = underTest.uiState.value.content.split("\n")
            assertThat(displayedLines).hasSize(1500)
            assertThat(underTest.uiState.value.hasMoreLines).isFalse()
            assertThat(displayedLines.last()).isEqualTo("line1500")
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile merges edited visible lines with original tail when content is capped`() =
        runTest {
            val allLines = (1..1500).map { "line$it" }
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

            assertThat(underTest.uiState.value.hasMoreLines).isTrue()
            val editedContent = (listOf("EDITED") + (2..1000).map { "line$it" }).joinToString("\n")
            underTest.saveFile(currentText = editedContent, fromHome = false)
            advanceUntilIdle()

            val textCaptor = argumentCaptor<String>()
            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), textCaptor.capture(), any(), any(), any(),
            )
            val savedLines = textCaptor.firstValue.split("\n")
            assertThat(savedLines.first()).isEqualTo("EDITED")
            assertThat(savedLines[999]).isEqualTo("line1000")
            assertThat(savedLines[1000]).isEqualTo("line1001")
            assertThat(savedLines.last()).isEqualTo("line1500")
            assertThat(savedLines).hasSize(1500)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile saves full content after onLoadMoreLines expands to all lines`() =
        runTest {
            val allLines = (1..1500).map { "line$it" }
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

            underTest.onLoadMoreLines()
            assertThat(underTest.uiState.value.appendSuffix).isNotNull()
            assertThat(underTest.uiState.value.hasMoreLines).isFalse()

            underTest.saveFile(
                currentText = allLines.joinToString("\n"),
                fromHome = false,
            )
            advanceUntilIdle()

            val textCaptor = argumentCaptor<String>()
            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), textCaptor.capture(), any(), any(), any(),
            )
            val savedLines = textCaptor.firstValue.split("\n")
            assertThat(savedLines).hasSize(1500)
            assertThat(savedLines.first()).isEqualTo("line1")
            assertThat(savedLines.last()).isEqualTo("line1500")
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile in Create mode saves state content as-is`() =
        runTest {
            val saveResult = TextEditorSaveResult.UploadRequired(
                tempPath = "/tmp/new.txt",
                parentHandle = 1L,
                isEditMode = false,
                fromHome = true,
            )
            whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
                .thenReturn(saveResult)
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Create)
            advanceUntilIdle()

            underTest.saveFile(currentText = "brand new content", fromHome = true)
            advanceUntilIdle()

            val textCaptor = argumentCaptor<String>()
            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), textCaptor.capture(), any(), any(), any(),
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
            underTest.saveFile(currentText = "edited content", fromHome = false)
            advanceUntilIdle()

            verify(saveTextContentForTextEditorUseCase).invoke(
                any(), any(), any(), any(), any(), any(),
            )
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
    fun `test that confirmDiscard restores content and switches to View mode`() =
        runTest {
            val lines = listOf("line1", "line2", "line3")
            doReturn(flowOf(lines)).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.confirmDiscard()
            advanceUntilIdle()

            val state = underTest.uiState.value
            assertThat(state.mode).isEqualTo(TextEditorMode.View)
            assertThat(state.content).isEqualTo(lines.joinToString("\n"))
            assertThat(state.isRestoringContent).isFalse()
            assertThat(state.showDiscardDialog).isFalse()
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

            underTest.saveFile(currentText = "new text", fromHome = false)
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

            underTest.saveFile(currentText = "new text", fromHome = false)
            advanceUntilIdle()
            assertThat(underTest.uiState.value.saveSuccessEvent).isEqualTo(triggered)

            underTest.consumeSaveSuccessEvent()
            assertThat(underTest.uiState.value.saveSuccessEvent).isEqualTo(consumed)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that setViewMode without unsaved changes switches directly to View`() =
        runTest {
            doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
                .invoke(any(), anyOrNull(), any())
            initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
            advanceUntilIdle()

            underTest.setViewMode()

            val state = underTest.uiState.value
            assertThat(state.mode).isEqualTo(TextEditorMode.View)
            assertThat(state.isRestoringContent).isFalse()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onLoadMoreLines emits appendSuffix in Edit mode`() = runTest {
        val allLines = (1..1500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.appendSuffix).isNull()

        underTest.onLoadMoreLines()

        val suffix = underTest.uiState.value.appendSuffix
        assertThat(suffix).isNotNull()
        val suffixLines = suffix!!.removePrefix("\n").split("\n")
        assertThat(suffixLines.first()).isEqualTo("line1001")
        assertThat(suffixLines.last()).isEqualTo("line1500")
        assertThat(underTest.uiState.value.hasMoreLines).isFalse()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that onLoadMoreLines updates content in View mode`() = runTest {
        val allLines = (1..1500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()

        assertThat(underTest.uiState.value.content.split("\n")).hasSize(1000)

        underTest.onLoadMoreLines()

        assertThat(underTest.uiState.value.appendSuffix).isNull()
        val displayedLines = underTest.uiState.value.content.split("\n")
        assertThat(displayedLines).hasSize(1500)
        assertThat(displayedLines.last()).isEqualTo("line1500")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that consumeAppendSuffix clears appendSuffix`() = runTest {
        val allLines = (1..1500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.onLoadMoreLines()
        assertThat(underTest.uiState.value.appendSuffix).isNotNull()

        underTest.consumeAppendSuffix()
        assertThat(underTest.uiState.value.appendSuffix).isNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that saveFile failure triggers errorEvent and sets loadErrorMessage`() = runTest {
        doReturn(flowOf(emptyList<String>())).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        whenever(saveTextContentForTextEditorUseCase(any(), any(), any(), any(), any(), any()))
            .thenThrow(RuntimeException("disk full"))

        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.saveFile(currentText = "some text", fromHome = false)
        advanceUntilIdle()

        val state = underTest.uiState.value
        assertThat(state.errorEvent).isEqualTo(triggered)
        assertThat(state.loadErrorMessage).isEqualTo("disk full")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that isContentDirty returns false after load-more appends suffix`() = runTest {
        val allLines = (1..1500).map { "line$it" }
        doReturn(flowOf(allLines)).whenever(getTextContentForTextEditorUseCase)
            .invoke(any(), anyOrNull(), any())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.Edit)
        advanceUntilIdle()

        underTest.onLoadMoreLines()

        val fullDisplayedContent = allLines.joinToString("\n")
        assertThat(underTest.isContentDirty(fullDisplayedContent)).isFalse()
    }
}
