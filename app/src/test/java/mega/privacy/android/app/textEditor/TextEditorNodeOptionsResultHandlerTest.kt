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
import mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER
import mega.privacy.android.app.utils.Constants.FOLDER_LINK_ADAPTER
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
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
    fun `test that TrashMenuAction returns false because the confirmation dialog drives the close`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(action = mock<TrashMenuAction>(), node = node)
            )
        ).isFalse()
    }

    @Test
    fun `test that DeletePermanentlyMenuAction returns false because the confirmation dialog drives the close`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult(
                    action = mock<DeletePermanentlyMenuAction>(),
                    node = node
                )
            )
        ).isFalse()
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

    @Test
    fun `test that buildTextEditorViewModelArgs routes a folder link as folder link even when publicUrl is set`() {
        val args = buildTextEditorViewModelArgs(
            LegacyTextEditorNavKey(
                nodeHandle = 123L,
                mode = TextEditorMode.View.value,
                nodeSourceType = FOLDER_LINK_ADAPTER,
                publicUrl = "https://mega.nz/folder/abc#key",
            )
        )

        // A folder link must NOT be treated as a file link: it resolves through the
        // FOLDER_LINK_ADAPTER path, and the folder URL is never forwarded as a file-link publicUrl.
        assertThat(args.isFolderLink).isTrue()
        assertThat(args.publicUrl).isNull()
        assertThat(args.nodeHandle).isEqualTo(123L)
    }

    @Test
    fun `test that buildTextEditorViewModelArgs routes a file link via publicUrl`() {
        val fileUrl = "https://mega.nz/file/abc#key"
        val args = buildTextEditorViewModelArgs(
            LegacyTextEditorNavKey(
                nodeHandle = 456L,
                mode = TextEditorMode.View.value,
                nodeSourceType = FILE_LINK_ADAPTER,
                publicUrl = fileUrl,
            )
        )

        assertThat(args.publicUrl).isEqualTo(fileUrl)
        assertThat(args.isFolderLink).isFalse()
    }
}
