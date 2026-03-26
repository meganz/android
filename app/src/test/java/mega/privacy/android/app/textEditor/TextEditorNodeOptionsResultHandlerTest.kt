package mega.privacy.android.app.textEditor

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.dialog.removelink.RemoveNodeLinkDialogNavKey
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheetMultiple
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TextEditorNodeOptionsResultHandlerTest {

    @Test
    fun `test that Navigation result returns true and editor should close`() {
        val navKey = mock<NavKey>()
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Navigation(
                    navKey
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that Transfer result returns true and editor should close`() {
        val event = TransferTriggerEvent.CopyOfflineNode(nodeIds = emptyList())
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Transfer(
                    event
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that Rename result returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Rename(
                    NodeId(1L)
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that null result returns false`() {
        assertThat(shouldCloseTextEditorOnNodeOptionsResult(null)).isFalse()
    }

    @Test
    fun `test that NodeNameCollision result returns false`() {
        val collisionResult = NodeNameCollisionsResult(
            noConflictNodes = emptyMap(),
            conflictNodes = emptyMap(),
            type = NodeNameCollisionType.COPY,
        )
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.NodeNameCollision(
                    collisionResult
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that shouldCloseTextEditorOnNodeOptionsResult returns false when navigation is ChangeLabelBottomSheet`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Navigation(
                    ChangeLabelBottomSheet(nodeId = 1L)
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that shouldCloseTextEditorOnNodeOptionsResult returns false when navigation is ChangeLabelBottomSheetMultiple`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Navigation(
                    ChangeLabelBottomSheetMultiple(nodeIds = listOf(1L, 2L))
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that shouldCloseTextEditorOnNodeOptionsResult returns false when navigation is RemoveNodeLinkDialogNavKey`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.Navigation(
                    RemoveNodeLinkDialogNavKey(nodes = "[1]")
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that AddToPlaylist result returns false`() {
        assertThat(
            shouldCloseTextEditorOnNodeOptionsResult(
                NodeOptionsBottomSheetResult.AddToPlaylist(
                    AddVideoToPlaylistResult()
                )
            )
        ).isFalse()
    }
}
