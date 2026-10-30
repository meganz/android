package mega.privacy.android.feature.texteditor.components.markdown.rich

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RichDocumentSelectionGestureTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that a block's select all is replaced with the document one`() {
        var documentSelectedAll = false
        val field = TextFieldState("alpha")
        val menu = blockContextMenu(field) { documentSelectedAll = true }

        menu.single { it.label == selectAllLabel() }.onClick(NoOpSession)

        assertThat(documentSelectedAll).isTrue()
        assertThat(field.selection.collapsed).isTrue()
    }

    @Test
    fun `test that a block's other menu items are left alone`() {
        val field = TextFieldState("alpha")

        val menu = blockContextMenu(field) {}

        assertThat(menu.map { it.label }).contains(selectAllLabel())
        assertThat(menu.map { it.label }).containsNoDuplicates()
    }

    @Test
    fun `test that a block menu without select all gains none`() {
        val field = TextFieldState("")

        val menu = blockContextMenu(field) {}

        assertThat(menu.map { it.label }).doesNotContain(selectAllLabel())
    }

    @Test
    fun `test that selectWholeDocument selects every block`() {
        val state = RichDocumentState(
            RichDocument(
                listOf(
                    RichBlock.Paragraph(RichText("first line")),
                    RichBlock.Paragraph(RichText("second line")),
                ),
            ),
        )
        val geometry = composedGeometry(state)

        runWithEditorScope { scope, clipboard ->
            selectWholeDocument(state, geometry, RecordingTextToolbar(), clipboard, scope)
        }

        assertThat(state.documentSelection).isEqualTo(
            RichDocSelection(
                RichDocPosition(0, 0),
                RichDocPosition(1, "second line".length),
            ),
        )
    }

    @Test
    fun `test that selectWholeDocument shows the document menu`() {
        val state = RichDocumentState(RichDocument(listOf(RichBlock.Paragraph(RichText("a")))))
        val geometry = composedGeometry(state)
        val toolbar = RecordingTextToolbar()

        runWithEditorScope { scope, clipboard ->
            selectWholeDocument(state, geometry, toolbar, clipboard, scope)
        }

        assertThat(toolbar.status).isEqualTo(TextToolbarStatus.Shown)
        assertThat(toolbar.onCopyRequested).isNotNull()
    }

    @Test
    fun `test that selectWholeDocument collapses the field selection it replaces`() {
        val state = RichDocumentState(RichDocument(listOf(RichBlock.Paragraph(RichText("word")))))
        val geometry = composedGeometry(state)
        val field = (state.blocks[0] as RichTextBlockEditState).text.textFieldState
        field.edit { selection = TextRange(0, 4) }
        state.focusedIndex = 0

        runWithEditorScope { scope, clipboard ->
            selectWholeDocument(state, geometry, RecordingTextToolbar(), clipboard, scope)
        }

        assertThat(field.selection.collapsed).isTrue()
    }

    @Test
    fun `test that the menu rect falls back to the composed blocks when both edges are out`() {
        val state = RichDocumentState(
            RichDocument(List(3) { RichBlock.Paragraph(RichText("line $it")) }),
        )
        val geometry = RichSelectionGeometry()
        geometry.fieldBounds[1] = Rect(10f, 100f, 200f, 140f)
        state.selectAllBlocks()

        assertThat(geometry.selectionMenuRect(state)).isEqualTo(Rect(10f, 100f, 200f, 140f))
    }

    @Test
    fun `test that the menu rect is null when no selected block is composed`() {
        val state = RichDocumentState(
            RichDocument(List(3) { RichBlock.Paragraph(RichText("line $it")) }),
        )
        val geometry = RichSelectionGeometry()
        state.selectAllBlocks()

        assertThat(geometry.selectionMenuRect(state)).isNull()
    }

    /**
     * Returns the context menu that a block with [field] gets under the editor's modifier. A
     * secondary click replaces the long press, because Robolectric cannot simulate a long press
     * that opens the menu.
     */
    private fun blockContextMenu(
        field: TextFieldState,
        onDocumentSelectAll: () -> Unit,
    ): List<TextContextMenuItem> {
        var menu: TextContextMenuData? = null
        val provider = object : TextContextMenuProvider {
            override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
                menu = dataProvider.data()
            }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalTextContextMenuDropdownProvider provides provider) {
                Box(modifier = Modifier.documentWideSelectAll(onDocumentSelectAll)) {
                    BasicTextField(
                        state = field,
                        modifier = Modifier.size(50.dp).testTag(BLOCK_TAG),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(BLOCK_TAG).performMouseInput { rightClick() }
        composeRule.waitForIdle()
        return checkNotNull(menu) { "The block showed no context menu" }
            .components
            .filterIsInstance<TextContextMenuItem>()
    }

    private fun selectAllLabel(): String =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getString(android.R.string.selectAll)

    /** Bounds for every block, as the composed rows register them. */
    private fun composedGeometry(state: RichDocumentState): RichSelectionGeometry =
        RichSelectionGeometry().apply {
            state.blocks.indices.forEach { index ->
                fieldBounds[index] = Rect(0f, index * 40f, 300f, index * 40f + 40f)
            }
        }

    /** Runs [block] with the editor's own scope and clipboard, then waits for the next frame. */
    private fun runWithEditorScope(block: (CoroutineScope, Clipboard) -> Unit) {
        lateinit var scope: CoroutineScope
        lateinit var clipboard: Clipboard
        composeRule.setContent {
            scope = rememberCoroutineScope()
            clipboard = LocalClipboard.current
        }
        composeRule.runOnIdle { block(scope, clipboard) }
        composeRule.waitForIdle()
    }

    private object NoOpSession : TextContextMenuSession {
        override fun close() = Unit
    }

    private class RecordingTextToolbar : TextToolbar {
        var onCopyRequested: (() -> Unit)? = null

        override var status: TextToolbarStatus = TextToolbarStatus.Hidden
            private set

        override fun hide() {
            status = TextToolbarStatus.Hidden
        }

        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?,
        ) {
            status = TextToolbarStatus.Shown
            this.onCopyRequested = onCopyRequested
        }
    }

    private companion object {
        const val BLOCK_TAG = "block"
    }
}
