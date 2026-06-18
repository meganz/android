package mega.privacy.android.feature.cloudexplorer.presentation.components

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.nodes.components.previewdata.previewFileNodeUiItem
import mega.privacy.android.shared.nodes.components.previewdata.previewFolderNodeUiItem
import mega.privacy.android.shared.nodes.selection.NodeSelectionState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExplorerNodeClickTest {

    @Test
    fun `test that clicking a folder navigates into it and clears the selection`() {
        var clickedFolder: NodeId? = null
        val selectionState = NodeSelectionState(initialSelectedIds = setOf(NodeId(99L)))
        val click = explorerNodeClick(
            selectionState = selectionState,
            disabledNodeIds = emptySet(),
            videosOnly = false,
            isSelectionModeEnabled = true,
            onFolderClick = { clickedFolder = it },
        )

        click(previewFolderNodeUiItem(FOLDER_ID.longValue))

        assertThat(clickedFolder).isEqualTo(FOLDER_ID)
        assertThat(selectionState.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that clicking a disabled node does nothing`() {
        var clickedFolder: NodeId? = null
        val selectionState = NodeSelectionState()
        val click = explorerNodeClick(
            selectionState = selectionState,
            disabledNodeIds = setOf(FOLDER_ID),
            videosOnly = false,
            isSelectionModeEnabled = true,
            onFolderClick = { clickedFolder = it },
        )

        click(previewFolderNodeUiItem(FOLDER_ID.longValue))

        assertThat(clickedFolder).isNull()
        assertThat(selectionState.selectedNodeIds).isEmpty()
    }

    @Test
    fun `test that clicking a file toggles its selection when selection mode is enabled`() {
        val selectionState = NodeSelectionState()
        val click = explorerNodeClick(
            selectionState = selectionState,
            disabledNodeIds = emptySet(),
            videosOnly = false,
            isSelectionModeEnabled = true,
            onFolderClick = {},
        )

        click(previewFileNodeUiItem(FILE_ID.longValue))

        assertThat(selectionState.selectedNodeIds).containsExactly(FILE_ID)
    }

    @Test
    fun `test that clicking a non-video file in videos-only mode does nothing`() {
        val selectionState = NodeSelectionState()
        val click = explorerNodeClick(
            selectionState = selectionState,
            disabledNodeIds = emptySet(),
            videosOnly = true,
            isSelectionModeEnabled = true,
            onFolderClick = {},
        )

        click(previewFileNodeUiItem(FILE_ID.longValue))

        assertThat(selectionState.selectedNodeIds).isEmpty()
    }

    private companion object {
        val FOLDER_ID = NodeId(1L)
        val FILE_ID = NodeId(10L)
    }
}
