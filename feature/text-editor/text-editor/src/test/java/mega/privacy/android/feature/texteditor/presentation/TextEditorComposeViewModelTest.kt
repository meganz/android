package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel.Args
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorConditionalTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlot
import mega.privacy.android.feature.texteditor.presentation.model.DefaultTextEditorTopBarSlots
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarSlots
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import mega.privacy.android.domain.usecase.texteditor.GetTextContentForTextEditorUseCase
import mega.privacy.android.domain.usecase.texteditor.SaveTextContentForTextEditorUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorComposeViewModelTest {

    private val getTextContentForTextEditorUseCase: GetTextContentForTextEditorUseCase = mock()
    private val saveTextContentForTextEditorUseCase: SaveTextContentForTextEditorUseCase = mock()

    private lateinit var underTest: TextEditorComposeViewModel

    @BeforeEach
    fun resetMocks() {
        reset(getTextContentForTextEditorUseCase, saveTextContentForTextEditorUseCase)
    }

    private fun initUnderTest(
        nodeHandle: Long = 0L,
        mode: TextEditorMode = TextEditorMode.View,
        nodeSourceType: Int? = null,
        fileName: String? = null,
        topBarSlots: TextEditorTopBarSlots = DefaultTextEditorTopBarSlots,
        localPath: String? = null,
    ) {
        underTest = TextEditorComposeViewModel(
            args = Args(
                nodeHandle = nodeHandle,
                mode = mode,
                nodeSourceType = nodeSourceType,
                fileName = fileName,
                topBarSlots = topBarSlots,
                localPath = localPath,
            ),
            getTextContentForTextEditorUseCase = getTextContentForTextEditorUseCase,
            saveTextContentForTextEditorUseCase = saveTextContentForTextEditorUseCase,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that initial uiState reflects Args`() = runTest {
        doReturn("").whenever(getTextContentForTextEditorUseCase).invoke(any(), any(), anyOrNull())
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "notes.txt",
        )
        advanceUntilIdle()
        val state = underTest.uiState.value
        assertThat(state.fileName).isEqualTo("notes.txt")
        assertThat(state.mode).isEqualTo(TextEditorMode.View)
        assertThat(state.isFileEdited).isFalse()
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
    fun `test that initial uiState reflects topBarSlots from Args`() {
        val slots: TextEditorTopBarSlots = listOf(
            TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.GetLink),
        )
        initUnderTest(topBarSlots = slots)
        val state = underTest.uiState.value
        assertThat(state.topBarSlots).containsExactly(
            TextEditorTopBarSlot.Conditional(TextEditorConditionalTopBarAction.GetLink),
        )
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

    @Test
    fun `test that onMenuAction Download GetLink Share do not change state`() {
        initUnderTest()
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

    @Test
    fun `test that updateContent sets content and isFileEdited`() {
        initUnderTest(mode = TextEditorMode.Edit)
        underTest.updateContent("new text")
        assertThat(underTest.uiState.value.content).isEqualTo("new text")
        assertThat(underTest.uiState.value.isFileEdited).isTrue()
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
        doThrow(RuntimeException("load failed"))
            .whenever(getTextContentForTextEditorUseCase).invoke(any(), any(), anyOrNull())
        initUnderTest(nodeHandle = 1L, mode = TextEditorMode.View)
        advanceUntilIdle()
        assertThat(underTest.uiState.value.errorEvent).isEqualTo(triggered)
        assertThat(underTest.uiState.value.isLoading).isFalse()
    }
}
