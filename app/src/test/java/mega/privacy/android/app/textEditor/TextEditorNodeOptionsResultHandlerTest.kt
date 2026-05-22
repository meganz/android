package mega.privacy.android.app.textEditor

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareDropdownMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
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
    fun `test that TrashMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<TrashMenuAction>(), node = node)
            )
        ).isTrue()
    }

    @Test
    fun `test that DeletePermanentlyMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(
                    action = mock<DeletePermanentlyMenuAction>(),
                    node = node
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that RemoveMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<RemoveMenuAction>(), node = node)
            )
        ).isTrue()
    }

    @Test
    fun `test that MoveMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<MoveMenuAction>(), node = node)
            )
        ).isTrue()
    }

    @Test
    fun `test that LeaveShareMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<LeaveShareMenuAction>(), node = node)
            )
        ).isTrue()
    }

    @Test
    fun `test that RemoveShareMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<RemoveShareMenuAction>(), node = node)
            )
        ).isTrue()
    }

    @Test
    fun `test that RemoveShareDropdownMenuAction returns true`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(
                    action = mock<RemoveShareDropdownMenuAction>(),
                    node = node
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that non-destructive action returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<ShareMenuAction>(), node = node)
            )
        ).isFalse()
    }

    @Test
    fun `test that unknown action returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<MenuAction>(), node = node)
            )
        ).isFalse()
    }
}
