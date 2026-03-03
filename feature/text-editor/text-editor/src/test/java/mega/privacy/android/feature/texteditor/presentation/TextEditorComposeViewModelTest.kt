package mega.privacy.android.feature.texteditor.presentation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel.Args
import mega.privacy.android.feature.texteditor.presentation.model.TextEditorTopBarAction
import org.junit.jupiter.api.Test

internal class TextEditorComposeViewModelTest {

    private lateinit var underTest: TextEditorComposeViewModel

    private fun initUnderTest(
        nodeHandle: Long = 0L,
        mode: TextEditorMode = TextEditorMode.View,
        nodeSourceType: Int? = null,
        fileName: String? = null,
    ) {
        underTest = TextEditorComposeViewModel(
            args = Args(
                nodeHandle = nodeHandle,
                mode = mode,
                nodeSourceType = nodeSourceType,
                fileName = fileName,
            )
        )
    }

    @Test
    fun `test that initial uiState reflects Args`() {
        initUnderTest(
            nodeHandle = 1L,
            mode = TextEditorMode.View,
            fileName = "notes.txt",
        )
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
    fun `test that onMenuAction LineNumbers toggles showLineNumbers`() {
        initUnderTest()
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        assertThat(underTest.uiState.value.showLineNumbers).isTrue()

        underTest.onMenuAction(TextEditorTopBarAction.LineNumbers)
        assertThat(underTest.uiState.value.showLineNumbers).isFalse()
    }
}
