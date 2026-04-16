package mega.privacy.android.app.textEditor

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorNodeOptionsResultHandlerTest {

    private val node = mock<TypedNode>()

    @Test
    fun `test that null result returns false`() {
        assertThat(shouldCloseTextEditorOnNodeOptionsResult(null)).isFalse()
    }

    @Test
    fun `test that LabelMenuAction result returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<LabelMenuAction>(), node = node)
            )
        ).isFalse()
    }

    @Test
    fun `test that RemoveLinkMenuAction result returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<RemoveLinkMenuAction>(), node = node)
            )
        ).isFalse()
    }

    @Test
    fun `test that OpenWithMenuAction result returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<OpenWithMenuAction>(), node = node)
            )
        ).isFalse()
    }

    @Test
    fun `test that other action result returns true and editor should close`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<MenuAction>(), node = node)
            )
        ).isTrue()
    }
}
